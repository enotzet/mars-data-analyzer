package app.service;

import app.dto.ChatMessageDto;
import app.dto.ChatResponse;
import app.dto.ChatSessionSummaryDto;
import app.model.ChatLog;
import app.repository.ChatLogRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatHistoryService {

    private static final int TITLE_MAX_LEN = 60;

    private final ChatLogRepository repo;

    public ChatHistoryService(ChatLogRepository repo) {
        this.repo = repo;
    }

    public List<ChatSessionSummaryDto> listSessions(String userId) {
        List<ChatLog> all = repo.findByUserIdOrderByTimestampDesc(userId);

        Map<String, List<ChatLog>> grouped = all.stream()
                .collect(Collectors.groupingBy(ChatLog::getSessionId, LinkedHashMap::new, Collectors.toList()));

        List<ChatSessionSummaryDto> summaries = new ArrayList<>();
        for (Map.Entry<String, List<ChatLog>> e : grouped.entrySet()) {
            List<ChatLog> entries = e.getValue();
            ChatLog newest = entries.get(0);
            ChatLog oldest = entries.get(entries.size() - 1);
            summaries.add(new ChatSessionSummaryDto(
                    e.getKey(),
                    truncate(oldest.getQuestion()),
                    newest.getTimestamp(),
                    entries.size(),
                    false
            ));
        }
        return summaries;
    }

    public List<ChatMessageDto> getSessionMessages(String sessionId) {
        return repo.findBySessionIdOrderByTimestampAsc(sessionId).stream()
                .map(this::toMessage)
                .collect(Collectors.toList());
    }

    private ChatMessageDto toMessage(ChatLog log) {
        List<ChatResponse.SourceEvidence> sources = log.getSources() == null
                ? List.of()
                : log.getSources().stream()
                        .map(s -> new ChatResponse.SourceEvidence(s.getUrl(), s.getSource(), s.getSnippet(), s.getScore()))
                        .collect(Collectors.toList());
        return new ChatMessageDto(
                log.getQuestion(),
                log.getAnswer(),
                sources,
                log.getTimestamp(),
                log.getJobId(),
                log.getPromptVersionId()
        );
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() <= TITLE_MAX_LEN ? text : text.substring(0, TITLE_MAX_LEN) + "...";
    }
}
