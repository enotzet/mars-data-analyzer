package app.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "prompt_templates")
public class PromptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private int version;

    @Column(columnDefinition = "TEXT")
    private String templateText;

    private boolean active;
    private Instant createdAt;

    public PromptTemplate() {
        this.createdAt = Instant.now();
    }

    public PromptTemplate(String name, int version, String templateText, boolean active) {
        this();
        this.name = name;
        this.version = version;
        this.templateText = templateText;
        this.active = active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getTemplateText() { return templateText; }
    public void setTemplateText(String templateText) { this.templateText = templateText; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
