package app.dto;

import java.util.List;

/**
 * Result of a benchmark run. Each {@link ModeResult} corresponds to one of the
 * four evaluation configurations (zero-shot, system-prompt, rag-baseline,
 * rag-reranked) and carries aggregate statistics computed across the full
 * twenty-query suite, including mean and standard deviation across N repeats
 * per query.
 */
public record BenchmarkResultDto(
    String runId,
    int totalQueries,
    int repeatsPerQuery,
    List<ModeResult> modes
) {
    public record ModeResult(
        String mode,
        double avgJudgeScore,           // 0–5 scale, semantic comparison against ground truth
        double stdJudgeScore,
        double avgPrecisionAtK,         // legacy keyword-overlap metric, retained for retrieval comparison
        double stdPrecisionAtK,
        double avgLatencyMs,
        double stdLatencyMs,
        double consistencyScore,        // fraction of queries that returned ≥1 source (RAG modes only)
        List<QueryStats> queryResults
    ) {}

    public record QueryStats(
        String query,
        double meanJudgeScore,
        double stdJudgeScore,
        double meanPrecisionAtK,
        double meanLatencyMs,
        double stdLatencyMs,
        int repeats,
        String exampleAnswer
    ) {}
}
