package app.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.List;

@Document(indexName = "mars_chat_logs")
public class ChatLog {
    @Id
    private String id;
    private String jobId;
    private String sessionId;
    private String userId;
    private boolean ragEnabled;
    private String question;
    private String answer;

    @Field(type = FieldType.Text)
    private String retrievedContext;

    private String promptVersionId;
    private String modelName;
    private double temperature;
    private int maxTokens;
    private long latencyMs;

    @Field(type = FieldType.Nested)
    private List<SourceReference> sources;

    @Field(type = FieldType.Date)
    private Instant timestamp;

    public ChatLog() {}

    public ChatLog(String sessionId, boolean ragEnabled, String question, String answer) {
        this.sessionId = sessionId;
        this.ragEnabled = ragEnabled;
        this.question = question;
        this.answer = answer;
        this.timestamp = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public boolean isRagEnabled() { return ragEnabled; }
    public void setRagEnabled(boolean ragEnabled) { this.ragEnabled = ragEnabled; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getRetrievedContext() { return retrievedContext; }
    public void setRetrievedContext(String retrievedContext) { this.retrievedContext = retrievedContext; }
    public String getPromptVersionId() { return promptVersionId; }
    public void setPromptVersionId(String promptVersionId) { this.promptVersionId = promptVersionId; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    public List<SourceReference> getSources() { return sources; }
    public void setSources(List<SourceReference> sources) { this.sources = sources; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public static class SourceReference {
        private String url;
        private String source;
        private String snippet;
        private double score;

        public SourceReference() {}
        public SourceReference(String url, String source, String snippet, double score) {
            this.url = url;
            this.source = source;
            this.snippet = snippet;
            this.score = score;
        }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getSnippet() { return snippet; }
        public void setSnippet(String snippet) { this.snippet = snippet; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
    }
}
