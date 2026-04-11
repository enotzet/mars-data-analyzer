package app.dto;

import java.util.List;

public record BenchmarkResultDto(
    String runId,
    String mode,
    int totalQueries,
    double avgPrecisionAtK,
    double avgLatencyMs,
    double consistencyScore,
    List<QueryResult> queryResults
) {
    public record QueryResult(
        String query,
        double precisionAtK,
        long latencyMs,
        int sourcesReturned,
        String answerSnippet
    ) {}
}
