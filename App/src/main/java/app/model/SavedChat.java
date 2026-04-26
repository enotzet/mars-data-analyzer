package app.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "saved_chats",
        uniqueConstraints = @UniqueConstraint(name = "uk_saved_chats_user_session", columnNames = {"user_id", "session_id"})
)
public class SavedChat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    private Instant savedAt;

    public SavedChat() {}

    public SavedChat(String userId, String sessionId) {
        this.userId = userId;
        this.sessionId = sessionId;
    }

    @PrePersist
    void onPersist() {
        this.savedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Instant getSavedAt() { return savedAt; }
    public void setSavedAt(Instant savedAt) { this.savedAt = savedAt; }
}
