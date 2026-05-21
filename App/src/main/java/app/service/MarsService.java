package app.service;

import app.dto.ChatResponse;

public interface MarsService {

    ChatResponse askQuestionWithEvidence(String userQuery, String sessionId, boolean ragEnabled, String userId);

    String askQuestion(String userQuery, String sessionId, boolean ragEnabled);
}
