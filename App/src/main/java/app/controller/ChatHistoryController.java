package app.controller;

import app.dto.ChatMessageDto;
import app.dto.ChatSessionSummaryDto;
import app.service.ChatHistoryService;
import app.service.SavedChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/mars/chats")
public class ChatHistoryController {

    private final ChatHistoryService historyService;
    private final SavedChatService savedChatService;

    public ChatHistoryController(ChatHistoryService historyService, SavedChatService savedChatService) {
        this.historyService = historyService;
        this.savedChatService = savedChatService;
    }

    @GetMapping
    public List<ChatSessionSummaryDto> listChats(@RequestParam(defaultValue = "default") String userId) {
        Set<String> savedIds = savedChatService.getSavedSessionIds(userId);
        return historyService.listSessions(userId).stream()
                .map(s -> new ChatSessionSummaryDto(
                        s.sessionId(), s.title(), s.lastActivity(), s.turnCount(),
                        savedIds.contains(s.sessionId())))
                .toList();
    }

    @GetMapping("/{sessionId}")
    public List<ChatMessageDto> getChatMessages(@PathVariable String sessionId) {
        return historyService.getSessionMessages(sessionId);
    }

    @PostMapping("/{sessionId}/save")
    public ResponseEntity<Void> saveChat(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "default") String userId) {
        savedChatService.save(userId, sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sessionId}/save")
    public ResponseEntity<Void> unsaveChat(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "default") String userId) {
        savedChatService.unsave(userId, sessionId);
        return ResponseEntity.noContent().build();
    }
}
