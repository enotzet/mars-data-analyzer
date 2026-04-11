package app.service;

import app.dto.BenchmarkResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BenchmarkService {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkService.class);
    private final RetrievalService retrievalService;
    private final MarsService marsService;

    // 20+ predefined benchmark queries with expected keywords for precision evaluation
    private static final List<BenchmarkQuery> BENCHMARK_QUERIES = List.of(
        new BenchmarkQuery("What geological features are visible on the Martian surface?", Set.of("rock", "crater", "sediment", "geological", "terrain")),
        new BenchmarkQuery("Describe the color of Mars soil", Set.of("red", "iron", "oxide", "soil", "dust", "color")),
        new BenchmarkQuery("What evidence of water exists on Mars?", Set.of("water", "ice", "erosion", "channel", "mineral")),
        new BenchmarkQuery("What does the Martian sky look like?", Set.of("sky", "atmosphere", "dust", "sunset", "blue")),
        new BenchmarkQuery("Describe craters on Mars surface", Set.of("crater", "impact", "rim", "basin")),
        new BenchmarkQuery("What rocks are found on Mars?", Set.of("rock", "basalt", "mineral", "volcanic", "igneous")),
        new BenchmarkQuery("How does Mars terrain compare to Earth?", Set.of("terrain", "earth", "mountain", "valley", "comparison")),
        new BenchmarkQuery("What is the composition of Martian dust?", Set.of("dust", "iron", "oxide", "particle", "composition")),
        new BenchmarkQuery("Describe sand dunes on Mars", Set.of("sand", "dune", "wind", "aeolian", "pattern")),
        new BenchmarkQuery("What volcanic features exist on Mars?", Set.of("volcano", "olympus", "lava", "volcanic", "caldera")),
        new BenchmarkQuery("Describe the Martian polar ice caps", Set.of("ice", "polar", "cap", "carbon", "dioxide", "water")),
        new BenchmarkQuery("What weather patterns occur on Mars?", Set.of("weather", "storm", "dust", "wind", "temperature")),
        new BenchmarkQuery("Describe Valles Marineris canyon system", Set.of("valley", "canyon", "marineris", "rift", "tectonic")),
        new BenchmarkQuery("What minerals have been found on Mars?", Set.of("mineral", "hematite", "olivine", "sulfate", "clay")),
        new BenchmarkQuery("How does the Curiosity rover navigate Mars terrain?", Set.of("rover", "curiosity", "wheel", "navigate", "terrain")),
        new BenchmarkQuery("What does the Mars surface look like at night?", Set.of("night", "dark", "star", "moon", "phobos")),
        new BenchmarkQuery("Describe sedimentary layers on Mars", Set.of("sediment", "layer", "strata", "deposit", "formation")),
        new BenchmarkQuery("What is the temperature on Mars surface?", Set.of("temperature", "cold", "celsius", "fahrenheit", "climate")),
        new BenchmarkQuery("Describe the Gale Crater exploration", Set.of("gale", "crater", "curiosity", "mount", "sharp")),
        new BenchmarkQuery("What signs of past habitability exist on Mars?", Set.of("habitable", "organic", "life", "biosignature", "water"))
    );

    public BenchmarkService(RetrievalService retrievalService, MarsService marsService) {
        this.retrievalService = retrievalService;
        this.marsService = marsService;
    }

    /** Run benchmark in a given mode (baseline or reranked). */
    public BenchmarkResultDto runBenchmark(String mode, String sessionId) {
        String runId = UUID.randomUUID().toString();
        List<BenchmarkResultDto.QueryResult> results = new ArrayList<>();
        boolean useReranking = "reranked".equalsIgnoreCase(mode);

        for (BenchmarkQuery bq : BENCHMARK_QUERIES) {
            long start = System.currentTimeMillis();
            try {
                // Retrieval step
                List<RetrievalService.ScoredDocument> docs = useReranking
                        ? retrievalService.hybridSearch(bq.query(), 3)
                        : retrievalService.baselineSearch(bq.query(), 3);

                // Compute Precision@K: how many retrieved docs contain expected keywords
                int relevant = 0;
                for (var sd : docs) {
                    String content = sd.document().getContent().toLowerCase();
                    long matchCount = bq.expectedKeywords().stream()
                            .filter(kw -> content.contains(kw.toLowerCase()))
                            .count();
                    if (matchCount >= 2) relevant++;
                }
                double precisionAtK = docs.isEmpty() ? 0.0 : relevant / (double) docs.size();

                // Get an answer for the query
                String answer = marsService.askQuestion(bq.query(), sessionId, true);
                long latency = System.currentTimeMillis() - start;

                results.add(new BenchmarkResultDto.QueryResult(
                    bq.query(),
                    precisionAtK,
                    latency,
                    docs.size(),
                    answer.substring(0, Math.min(200, answer.length()))
                ));

            } catch (Exception e) {
                long latency = System.currentTimeMillis() - start;
                log.error("Benchmark query failed: {}", e.getMessage());
                results.add(new BenchmarkResultDto.QueryResult(bq.query(), 0.0, latency, 0, "ERROR: " + e.getMessage()));
            }
        }

        double avgPrecision = results.stream().mapToDouble(BenchmarkResultDto.QueryResult::precisionAtK).average().orElse(0);
        double avgLatency = results.stream().mapToDouble(BenchmarkResultDto.QueryResult::latencyMs).average().orElse(0);
        double consistency = computeConsistency(results);

        return new BenchmarkResultDto(runId, mode, results.size(), avgPrecision, avgLatency, consistency, results);
    }

    /** Consistency: ratio of queries that returned at least one source. */
    private double computeConsistency(List<BenchmarkResultDto.QueryResult> results) {
        if (results.isEmpty()) return 0.0;
        long withSources = results.stream().filter(r -> r.sourcesReturned() > 0).count();
        return withSources / (double) results.size();
    }

    private record BenchmarkQuery(String query, Set<String> expectedKeywords) {}
}
