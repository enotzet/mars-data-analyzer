package app.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "api_usage")
public class ApiUsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;
    private String endpoint;
    private Instant timestamp;

    public ApiUsageRecord() {
        this.timestamp = Instant.now();
    }

    public ApiUsageRecord(String userId, String endpoint) {
        this();
        this.userId = userId;
        this.endpoint = endpoint;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
