package app.service;

import app.model.ChatLog;
import app.repository.ChatLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ProvenanceService {

    private static final Logger log = LoggerFactory.getLogger(ProvenanceService.class);
    private final ChatLogRepository chatLogRepository;

    public ProvenanceService(ChatLogRepository chatLogRepository) {
        this.chatLogRepository = chatLogRepository;
    }

    public void logInteraction(
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
    ) {
        try {
            ChatLog entry = new ChatLog();
            entry.setJobId(jobId);
            entry.setSessionId(sessionId);
            entry.setUserId(userId);
            entry.setRagEnabled(ragEnabled);
            entry.setQuestion(question);
            entry.setAnswer(answer);
            entry.setRetrievedContext(retrievedContext);
            entry.setPromptVersionId(promptVersionId);
            entry.setModelName(modelName);
            entry.setTemperature(temperature);
            entry.setMaxTokens(maxTokens);
            entry.setLatencyMs(latencyMs);
            entry.setSources(sources);
            entry.setTimestamp(Instant.now());
            chatLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to save provenance log to Elasticsearch: {}", e.getMessage());
        }
    }
}
