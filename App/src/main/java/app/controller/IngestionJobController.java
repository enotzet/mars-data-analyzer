package app.controller;

import app.dto.JobStatusDto;
import app.model.IngestionJob;
import app.service.IngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mars")
public class IngestionJobController {

    private final IngestionService ingestionService;

    public IngestionJobController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<JobStatusDto> ingest(@RequestBody(required = false) Map<String, String> body) {
        String source = (body != null && body.containsKey("source")) ? body.get("source") : "all";
        IngestionJob job = ingestionService.submitJob(source);
        return ResponseEntity.accepted().body(toDto(job));
    }

    @PostMapping("/ingest/{jobId}/retry")
    public ResponseEntity<JobStatusDto> retryIngest(@PathVariable String jobId) {
        IngestionJob job = ingestionService.retryJob(jobId);
        return ResponseEntity.accepted().body(toDto(job));
    }

    @GetMapping("/jobs")
    public List<JobStatusDto> listJobs() {
        return ingestionService.getAllJobs().stream().map(this::toDto).toList();
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<JobStatusDto> getJob(@PathVariable String jobId) {
        return ingestionService.getJob(jobId)
                .map(j -> ResponseEntity.ok(toDto(j)))
                .orElse(ResponseEntity.notFound().build());
    }

    private JobStatusDto toDto(IngestionJob job) {
        return new JobStatusDto(
                job.getId(), job.getStatus().name(), job.getSource(),
                job.getTotalItems(), job.getProcessedItems(), job.getFailedItems(),
                job.getErrorMessage(), job.getRetryCount(),
                job.getCreatedAt(), job.getUpdatedAt()
        );
    }
}
