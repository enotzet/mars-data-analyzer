package app.dto;

import java.time.Instant;
import java.util.List;

public record ChatMessageDto(
        String question,
        String answer,
        List<ChatResponse.SourceEvidence> sources,
        Instant timestamp,
        String jobId,
        String promptVersionId
) {}
