package app.controller;

import app.dto.BenchmarkResultDto;
import app.service.BenchmarkService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    @PostMapping("/run")
    public BenchmarkResultDto runBenchmark(
            @RequestParam(defaultValue = "reranked") String mode,
            @RequestParam(defaultValue = "benchmark-session") String sessionId) {
        return benchmarkService.runBenchmark(mode, sessionId);
    }
}
