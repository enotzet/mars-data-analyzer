package app.controller;

import app.dto.ChatRequest;
import app.dto.ChatResponse;
import app.dto.JobStatusDto;
import app.model.IngestionJob;
import app.service.IngestionService;
import app.service.MarsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mars")
public class MarsController {

    private final MarsService marsService;
    private final IngestionService ingestionService;

    public MarsController(MarsService marsService, IngestionService ingestionService) {
        this.marsService = marsService;
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

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return marsService.askQuestionWithEvidence(
                request.question(),
                request.sessionId(),
                request.ragEnabled(),
                request.userId()
        );
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
