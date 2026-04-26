package app.repository;

import app.model.SavedChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedChatRepository extends JpaRepository<SavedChat, String> {

    List<SavedChat> findByUserIdOrderBySavedAtDesc(String userId);

    Optional<SavedChat> findByUserIdAndSessionId(String userId, String sessionId);
}
