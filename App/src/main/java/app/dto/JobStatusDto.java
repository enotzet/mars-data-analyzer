package app.dto;

import java.time.Instant;

public record JobStatusDto(
    String id,
    String status,
    String source,
    int totalItems,
    int processedItems,
    int failedItems,
    String errorMessage,
    int retryCount,
    Instant createdAt,
    Instant updatedAt
) {}
