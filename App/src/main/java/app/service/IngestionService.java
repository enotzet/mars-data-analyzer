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

    private final IngestionJobRepository jobRepository;
    private final NasaApiService nasaApiService;
    private final RetrievalService retrievalService;
    private final PromptService promptService;
    private final ChatModel chatModel;
    private final RabbitTemplate rabbitTemplate;

    public IngestionService(
            IngestionJobRepository jobRepository,
            NasaApiService nasaApiService,
            RetrievalService retrievalService,
            PromptService promptService,
            ChatModel chatModel,
            RabbitTemplate rabbitTemplate) {
        this.jobRepository = jobRepository;
        this.nasaApiService = nasaApiService;
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
            List<NasaApiService.ImageRecord> images = fetchImagesForSource(job.getSource());
            job.setTotalItems(Math.min(images.size(), MAX_IMAGES_PER_JOB));
            jobRepository.save(job);

            int processed = 0;
            int failed = 0;

            for (NasaApiService.ImageRecord img : images) {
                if (processed + failed >= MAX_IMAGES_PER_JOB) break;

                try {
                    // Deduplication check
                    if (retrievalService.documentExistsByUrl(img.imageUrl())) {
                        log.info("Skipping duplicate: {}", img.imageUrl());
                        continue;
                    }

                    byte[] imageBytes = nasaApiService.downloadImage(img.imageUrl());
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

        } catch (Exception e) {
            log.error("Job {} failed: {}", jobId, e.getMessage());
            job.setStatus(IngestionJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            jobRepository.save(job);
        }
    }

    private List<NasaApiService.ImageRecord> fetchImagesForSource(String source) {
        return switch (source) {
            case "nasa-image-library" -> nasaApiService.fetchNasaImageLibrary();
            case "mars-rover-photos" -> nasaApiService.fetchMarsRoverPhotos();
            case "all" -> {
                List<NasaApiService.ImageRecord> all = new ArrayList<>();
                all.addAll(nasaApiService.fetchNasaImageLibrary());
                all.addAll(nasaApiService.fetchMarsRoverPhotos());
                yield all;
            }
            default -> nasaApiService.fetchNasaImageLibrary();
        };
    }

    private double computeQualityScore(byte[] imageBytes, NasaApiService.ImageRecord record) {
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
