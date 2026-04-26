package app.dto;

import java.time.Instant;

public record ChatSessionSummaryDto(
        String sessionId,
        String title,
        Instant lastActivity,
        int turnCount,
        boolean saved
) {}
