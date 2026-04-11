package app.dto;

public record ChatRequest(
    String question,
    String sessionId,
    boolean ragEnabled,
    String userId
) {}
