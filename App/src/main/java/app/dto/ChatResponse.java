package app.dto;

import java.util.List;

public record ChatResponse(
    String answer,
    String jobId,
    String promptVersionId,
    List<SourceEvidence> sources
) {
    public record SourceEvidence(
        String url,
        String sourceName,
        String snippet,
        double relevanceScore
    ) {}
}
