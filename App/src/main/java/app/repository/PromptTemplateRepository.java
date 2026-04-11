package app.repository;

import app.model.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, String> {
    Optional<PromptTemplate> findByNameAndActiveTrue(String name);
    List<PromptTemplate> findByNameOrderByVersionDesc(String name);
    Optional<PromptTemplate> findByNameAndVersion(String name, int version);
}
