package app.service;

import app.model.PromptTemplate;
import app.repository.PromptTemplateRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PromptService {

    private static final Logger log = LoggerFactory.getLogger(PromptService.class);
    private final PromptTemplateRepository repo;

    public static final String IMAGE_ANALYSIS = "image-analysis";
    public static final String RAG_CHAT = "rag-chat";
    public static final String GENERAL_CHAT = "general-chat";

    public PromptService(PromptTemplateRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void seedDefaults() {
        seedIfMissing(IMAGE_ANALYSIS, 1, """
            You are an astronomical expert with extensive knowledge of Mars. \
            Describe in detail everything you see in this image of the Martian surface. \
            Include geological features, color observations, and any notable formations.""");

        seedIfMissing(RAG_CHAT, 1, """
            You are a Mars expert. Use ONLY the provided Context to answer the user question. \
            For each claim you make, cite the source by referencing its [Source N] tag. \
            If the context does not contain enough information, say so explicitly.

            Context:
            {context}""");

        seedIfMissing(GENERAL_CHAT, 1,
            "You are a Mars expert. Answer the user's questions based on your general knowledge. " +
            "Be precise and scientific in your responses.");
    }

    private void seedIfMissing(String name, int version, String text) {
        if (repo.findByNameAndActiveTrue(name).isEmpty()) {
            repo.save(new PromptTemplate(name, version, text, true));
            log.info("Seeded prompt template: {} v{}", name, version);
        }
    }

    public PromptTemplate getActivePrompt(String name) {
        return repo.findByNameAndActiveTrue(name)
                .orElseThrow(() -> new IllegalStateException("No active prompt template for: " + name));
    }

    public PromptTemplate createNewVersion(String name, String templateText) {
        Optional<PromptTemplate> current = repo.findByNameAndActiveTrue(name);
        int nextVersion = 1;
        if (current.isPresent()) {
            nextVersion = current.get().getVersion() + 1;
            current.get().setActive(false);
            repo.save(current.get());
        }
        PromptTemplate newTemplate = new PromptTemplate(name, nextVersion, templateText, true);
        return repo.save(newTemplate);
    }

    public List<PromptTemplate> getVersionHistory(String name) {
        return repo.findByNameOrderByVersionDesc(name);
    }
}
