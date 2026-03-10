package app.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Document(indexName = "mars_chat_logs")
public class ChatLog {
    @Id
    private String id;
    private String sessionId;
    private boolean ragEnabled;
    private String question;
    private String answer;

    @Field(type = FieldType.Date)
    private Instant timestamp;

    public ChatLog() {
    }

    public ChatLog(String sessionId, boolean ragEnabled, String question, String answer) {
        this.sessionId = sessionId;
        this.ragEnabled = ragEnabled;
        this.question = question;
        this.answer = answer;
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId( String id ) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId( String sessionId ) {
        this.sessionId = sessionId;
    }

    public boolean isRagEnabled() {
        return ragEnabled;
    }

    public void setRagEnabled( boolean ragEnabled ) {
        this.ragEnabled = ragEnabled;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion( String question ) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer( String answer ) {
        this.answer = answer;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp( Instant timestamp ) {
        this.timestamp = timestamp;
    }
}