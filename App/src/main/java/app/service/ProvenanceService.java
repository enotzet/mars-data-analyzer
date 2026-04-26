package app.service;

import app.model.ChatLog;

import java.util.List;

public interface ProvenanceService {

    void logInteraction(
            String jobId,
            String sessionId,
            String userId,
            boolean ragEnabled,
            String question,
            String answer,
            String retrievedContext,
            String promptVersionId,
            String modelName,
            double temperature,
            int maxTokens,
            long latencyMs,
            List<ChatLog.SourceReference> sources
    );
}
