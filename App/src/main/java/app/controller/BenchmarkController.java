package app.controller;

import app.dto.BenchmarkResultDto;
import app.service.BenchmarkService;
import app.service.MarsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    /**
     * Run the full four-mode benchmark with N repeats per (query, mode) pair.
     * Default N = 5. Single endpoint that produces the headline ladder.
     */
    @PostMapping("/run")
    public BenchmarkResultDto runBenchmark(
            @RequestParam(defaultValue = "5") int repeats) {
        return benchmarkService.runFullBenchmark(repeats);
    }

    /** Single-mode endpoint, for ad-hoc inspection of one configuration. */
    @PostMapping("/run-single")
    public BenchmarkResultDto.ModeResult runSingleMode(
            @RequestParam(defaultValue = "rag-reranked") String mode,
            @RequestParam(defaultValue = "5") int repeats) {
        MarsService.BenchmarkMode m = parseMode(mode);
        return benchmarkService.runSingleMode(m, repeats);
    }

    private MarsService.BenchmarkMode parseMode(String s) {
        return switch (s.toLowerCase().replace('-', '_')) {
            case "zero_shot" -> MarsService.BenchmarkMode.ZERO_SHOT;
            case "system_prompt" -> MarsService.BenchmarkMode.SYSTEM_PROMPT;
            case "rag_baseline", "baseline" -> MarsService.BenchmarkMode.RAG_BASELINE;
            case "rag_reranked", "reranked" -> MarsService.BenchmarkMode.RAG_RERANKED;
            default -> throw new IllegalArgumentException("Unknown benchmark mode: " + s);
        };
    }
}
