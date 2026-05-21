package app.service;

import app.model.SavedChat;
import app.repository.SavedChatRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SavedChatService {

    private final SavedChatRepository repo;

    public SavedChatService(SavedChatRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public SavedChat save(String userId, String sessionId) {
        return repo.findByUserIdAndSessionId(userId, sessionId)
                .orElseGet(() -> {
                    try {
                        return repo.saveAndFlush(new SavedChat(userId, sessionId));
                    } catch (DataIntegrityViolationException e) {
                        return repo.findByUserIdAndSessionId(userId, sessionId)
                                .orElseThrow(() -> e);
                    }
                });
    }

    @Transactional
    public void unsave(String userId, String sessionId) {
        repo.findByUserIdAndSessionId(userId, sessionId).ifPresent(repo::delete);
    }

    public List<SavedChat> list(String userId) {
        return repo.findByUserIdOrderBySavedAtDesc(userId);
    }

    public Set<String> getSavedSessionIds(String userId) {
        return list(userId).stream()
                .map(SavedChat::getSessionId)
                .collect(Collectors.toSet());
    }
}
