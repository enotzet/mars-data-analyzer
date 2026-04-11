package app.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ingestion_jobs")
public class IngestionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDING;

    private String source;
    private int totalItems;
    private int processedItems;
    private int failedItems;
    private String errorMessage;
    private int retryCount;
    private Instant createdAt;
    private Instant updatedAt;

    public enum JobStatus { PENDING, RUNNING, COMPLETED, FAILED }

    public IngestionJob() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public IngestionJob(String source) {
        this();
        this.source = source;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; this.updatedAt = Instant.now(); }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    public int getProcessedItems() { return processedItems; }
    public void setProcessedItems(int processedItems) { this.processedItems = processedItems; }
    public int getFailedItems() { return failedItems; }
    public void setFailedItems(int failedItems) { this.failedItems = failedItems; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
