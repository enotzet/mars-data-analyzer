package app.service;

import app.configuration.RabbitMQConfig;
import app.model.IngestionJob;
import app.repository.IngestionJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.*;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final int MAX_IMAGES_PER_JOB = 5;
    private static final int MIN_IMAGE_SIZE = 2048;

    // Safety net for residual model refusals. The image-analysis prompt instructs the
    // model to describe charts/diagrams instead of refusing, so we keep this list narrow:
    // only true "I cannot describe the image" refusals, never legitimate descriptions of
    // non-surface content (graphs, instruments, etc.).
    private static final List<String> INVALID_ANALYSIS_MARKERS = List.of(
            "can't analyze",
            "cannot analyze",
            "unable to analyze",
            "i'm unable to",
            "i am unable to",
            "i can't describe",
            "i cannot describe",
            "i'm sorry, but i can't",
            "i'm sorry, i can't"
    );

    private final IngestionJobRepository jobRepository;
    private final NasaApiAdapter nasaApiAdapter;
    private final InSightWeatherAdapter inSightWeatherAdapter;
    private final RetrievalService retrievalService;
    private final PromptService promptService;
    private final ChatModel chatModel;
    private final RabbitTemplate rabbitTemplate;

    public IngestionService(
            IngestionJobRepository jobRepository,
            NasaApiAdapter nasaApiAdapter,
            InSightWeatherAdapter inSightWeatherAdapter,
            RetrievalService retrievalService,
            PromptService promptService,
            ChatModel chatModel,
            RabbitTemplate rabbitTemplate) {
        this.jobRepository = jobRepository;
        this.nasaApiAdapter = nasaApiAdapter;
        this.inSightWeatherAdapter = inSightWeatherAdapter;
        this.retrievalService = retrievalService;
        this.promptService = promptService;
        this.chatModel = chatModel;
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Submit a new ingestion job to the queue. Returns job ID immediately. */
    public IngestionJob submitJob(String source) {
        IngestionJob job = new IngestionJob(source);
        job = jobRepository.save(job);
        rabbitTemplate.convertAndSend(RabbitMQConfig.INGESTION_QUEUE, job.getId());
        log.info("Submitted ingestion job {} for source: {}", job.getId(), source);
        return job;
    }

    /** Retry a failed job. */
    public IngestionJob retryJob(String jobId) {
        IngestionJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        if (job.getStatus() != IngestionJob.JobStatus.FAILED) {
            throw new IllegalStateException("Can only retry FAILED jobs");
        }
        job.setStatus(IngestionJob.JobStatus.PENDING);
        job.setRetryCount(job.getRetryCount() + 1);
        job.setErrorMessage(null);
        job = jobRepository.save(job);
        rabbitTemplate.convertAndSend(RabbitMQConfig.INGESTION_QUEUE, job.getId());
        return job;
    }

    public List<IngestionJob> getAllJobs() {
        return jobRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<IngestionJob> getJob(String jobId) {
        return jobRepository.findById(jobId);
    }

    /** RabbitMQ consumer: processes ingestion jobs asynchronously. */
    @RabbitListener(queues = RabbitMQConfig.INGESTION_QUEUE)
    public void processJob(String jobId) {
        IngestionJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error("Job not found: {}", jobId);
            return;
        }

        // Idempotency: skip if already completed
        if (job.getStatus() == IngestionJob.JobStatus.COMPLETED) {
            log.info("Job {} already completed, skipping", jobId);
            return;
        }

        job.setStatus(IngestionJob.JobStatus.RUNNING);
        jobRepository.save(job);

        try {
            if ("insight-weather".equals(job.getSource())) {
                processWeatherJob(job);
            } else {
                processImageJob(job);
            }
        } catch (Exception e) {
            log.error("Job {} failed: {}", jobId, e.getMessage());
            job.setStatus(IngestionJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            jobRepository.save(job);
        }
    }

    private void processImageJob(IngestionJob job) {
        String jobId = job.getId();
        List<NasaApiAdapter.ImageRecord> images = fetchImagesForSource(job.getSource());
        job.setTotalItems(Math.min(images.size(), MAX_IMAGES_PER_JOB));
        jobRepository.save(job);

        int processed = 0;
        int failed = 0;

        for (NasaApiAdapter.ImageRecord img : images) {
            if (processed + failed >= MAX_IMAGES_PER_JOB) break;

            try {
                // Deduplication check
                if (retrievalService.documentExistsByUrl(img.imageUrl())) {
                    log.info("Skipping duplicate: {}", img.imageUrl());
                    continue;
                }

                byte[] imageBytes = nasaApiAdapter.downloadImage(img.imageUrl());
                if (imageBytes == null || imageBytes.length < MIN_IMAGE_SIZE) {
                    log.warn("Skipping small/null image: {}", img.imageUrl());
                    failed++;
                    continue;
                }

                // Quality score based on image size and metadata completeness
                double qualityScore = computeQualityScore(imageBytes, img);

                // Analyze image with vision model
                String promptText = promptService.getActivePrompt(PromptService.IMAGE_ANALYSIS).getTemplateText();
                var userMessage = new UserMessage(promptText,
                        List.of(new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(imageBytes))));

                Prompt prompt = new Prompt(userMessage,
                        OpenAiChatOptions.builder()
                                .withTemperature(0.7F)
                                .withMaxTokens(1500)
                                .build());

                String description = chatModel.call(prompt).getResult().getOutput().getContent();

                if (isInvalidAnalysis(description)) {
                    log.warn("Skipping invalid analysis for {}: {}", img.imageUrl(),
                            description == null ? "<empty>"
                                    : description.substring(0, Math.min(120, description.length())).replaceAll("\\s+", " "));
                    failed++;
                    continue;
                }

                // Store with rich metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("url", img.imageUrl());
                metadata.put("source", img.source());
                metadata.put("title", img.title());
                metadata.put("externalId", img.externalId());
                metadata.put("qualityScore", String.valueOf(qualityScore));
                metadata.put("jobId", jobId);

                retrievalService.storeDocument(description, metadata);
                processed++;

                job.setProcessedItems(processed);
                jobRepository.save(job);
                log.info("Processed image {}/{}: {}", processed, job.getTotalItems(), img.imageUrl());

            } catch (Exception e) {
                log.error("Failed to process image {}: {}", img.imageUrl(), e.getMessage());
                failed++;
            }
        }

        job.setProcessedItems(processed);
        job.setFailedItems(failed);
        job.setStatus(IngestionJob.JobStatus.COMPLETED);
        jobRepository.save(job);
        log.info("Job {} completed: {} processed, {} failed", jobId, processed, failed);
    }

    private void processWeatherJob(IngestionJob job) {
        String jobId = job.getId();
        List<InSightWeatherAdapter.WeatherRecord> sols = inSightWeatherAdapter.fetchWeather();
        job.setTotalItems(sols.size());
        jobRepository.save(job);

        int processed = 0;
        int failed = 0;

        for (InSightWeatherAdapter.WeatherRecord rec : sols) {
            String solUrl = "https://api.nasa.gov/insight_weather/sol/" + rec.sol();
            try {
                if (retrievalService.documentExistsByUrl(solUrl)) {
                    log.info("Skipping duplicate weather sol: {}", rec.sol());
                    continue;
                }

                String description = formatWeatherRecord(rec);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("url", solUrl);
                metadata.put("source", "insight-weather");
                metadata.put("title", "InSight Mars Weather Sol " + rec.sol());
                metadata.put("externalId", "insight-sol-" + rec.sol());
                metadata.put("qualityScore", "0.95");
                metadata.put("jobId", jobId);

                retrievalService.storeDocument(description, metadata);
                processed++;

                job.setProcessedItems(processed);
                jobRepository.save(job);
                log.info("Processed weather sol {}/{}: sol {}", processed, job.getTotalItems(), rec.sol());

            } catch (Exception e) {
                log.error("Failed to process weather sol {}: {}", rec.sol(), e.getMessage());
                failed++;
            }
        }

        job.setProcessedItems(processed);
        job.setFailedItems(failed);
        job.setStatus(IngestionJob.JobStatus.COMPLETED);
        jobRepository.save(job);
        log.info("Weather job {} completed: {} processed, {} failed", jobId, processed, failed);
    }

    private List<NasaApiAdapter.ImageRecord> fetchImagesForSource(String source) {
        return switch (source) {
            case "nasa-image-library" -> nasaApiAdapter.fetchNasaImageLibrary();
            case "mars-rover-photos" -> nasaApiAdapter.fetchMarsRoverPhotos();
            case "all" -> {
                List<NasaApiAdapter.ImageRecord> all = new ArrayList<>();
                all.addAll(nasaApiAdapter.fetchNasaImageLibrary());
                all.addAll(nasaApiAdapter.fetchMarsRoverPhotos());
                yield all;
            }
            default -> nasaApiAdapter.fetchNasaImageLibrary();
        };
    }

    private String formatWeatherRecord(InSightWeatherAdapter.WeatherRecord r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mars weather telemetry from NASA's InSight lander at Elysium Planitia for Sol ")
                .append(r.sol());
        if (r.season() != null) sb.append(" (").append(r.season()).append(" season)");
        if (r.firstUtc() != null && r.lastUtc() != null) {
            sb.append(", measurement window ").append(r.firstUtc()).append(" to ").append(r.lastUtc()).append(" UTC");
        }
        sb.append(".\n\n");

        if (r.avgTempC() != null) {
            sb.append("Atmospheric temperature: average ")
                    .append(formatNum(r.avgTempC())).append(" \u00B0C");
            if (r.minTempC() != null && r.maxTempC() != null) {
                sb.append(", ranging from ").append(formatNum(r.minTempC()))
                        .append(" \u00B0C (coldest) to ").append(formatNum(r.maxTempC())).append(" \u00B0C (warmest)");
            }
            sb.append(".\n");
        }
        if (r.avgWindMs() != null) {
            sb.append("Horizontal wind speed: average ").append(formatNum(r.avgWindMs())).append(" m/s");
            if (r.maxWindMs() != null) sb.append(", peak ").append(formatNum(r.maxWindMs())).append(" m/s");
            sb.append(".\n");
        }
        if (r.avgPressurePa() != null) {
            sb.append("Surface pressure: average ").append(formatNum(r.avgPressurePa())).append(" Pa");
            if (r.minPressurePa() != null && r.maxPressurePa() != null) {
                sb.append(", ranging from ").append(formatNum(r.minPressurePa()))
                        .append(" to ").append(formatNum(r.maxPressurePa())).append(" Pa");
            }
            sb.append(".\n");
        }
        if (r.prevailingWindDir() != null) {
            sb.append("Prevailing wind direction: ").append(r.prevailingWindDir()).append(".\n");
        }

        sb.append("\nSource: NASA InSight Mars Weather Service (https://api.nasa.gov/insight_weather/).");
        return sb.toString();
    }

    private static String formatNum(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private boolean isInvalidAnalysis(String description) {
        if (description == null || description.isBlank()) return true;
        String lower = description.toLowerCase();
        return INVALID_ANALYSIS_MARKERS.stream().anyMatch(lower::contains);
    }

    private double computeQualityScore(byte[] imageBytes, NasaApiAdapter.ImageRecord record) {
        double score = 0.0;
        // Size component: larger images score higher (up to 0.4)
        if (imageBytes.length > 100_000) score += 0.4;
        else if (imageBytes.length > 50_000) score += 0.3;
        else if (imageBytes.length > 10_000) score += 0.2;
        else score += 0.1;

        // Metadata completeness (up to 0.3)
        if (record.title() != null && !record.title().isBlank()) score += 0.1;
        if (record.description() != null && !record.description().isBlank()) score += 0.1;
        if (record.externalId() != null && !record.externalId().isBlank()) score += 0.1;

        // Source trust (up to 0.3)
        if ("mars-rover-photos".equals(record.source())) score += 0.3;
        else if ("nasa-image-library".equals(record.source())) score += 0.25;
        else score += 0.1;

        return Math.min(score, 1.0);
    }
}
