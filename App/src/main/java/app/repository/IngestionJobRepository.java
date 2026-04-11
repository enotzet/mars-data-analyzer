package app.repository;

import app.model.IngestionJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngestionJobRepository extends JpaRepository<IngestionJob, String> {
    List<IngestionJob> findAllByOrderByCreatedAtDesc();
    List<IngestionJob> findByStatus(IngestionJob.JobStatus status);
}
