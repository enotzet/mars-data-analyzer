package app.service;

import app.configuration.PromptProperties;
import app.model.PromptTemplate;
import app.repository.PromptTemplateRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PromptServiceImpl implements PromptService {

    private static final Logger log = LoggerFactory.getLogger(PromptServiceImpl.class);
    private final PromptTemplateRepository repo;
    private final PromptProperties promptProperties;

    public PromptServiceImpl(PromptTemplateRepository repo, PromptProperties promptProperties) {
        this.repo = repo;
        this.promptProperties = promptProperties;
    }

    @PostConstruct
    public void seedDefaults() {
        promptProperties.getTemplates().forEach((name, text) -> seedIfMissing(name, 1, text));
    }

    private void seedIfMissing(String name, int version, String text) {
        if (repo.findByNameAndActiveTrue(name).isEmpty()) {
            repo.save(new PromptTemplate(name, version, text, true));
            log.info("Seeded prompt template: {} v{}", name, version);
        }
    }

    @Override
    public PromptTemplate getActivePrompt(String name) {
        return repo.findByNameAndActiveTrue(name)
                .orElseThrow(() -> new IllegalStateException("No active prompt template for: " + name));
    }

    @Override
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

    @Override
    public List<PromptTemplate> getVersionHistory(String name) {
        return repo.findByNameOrderByVersionDesc(name);
    }
}
