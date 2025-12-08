package app.controller;

import app.service.MarsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mars")
public class MarsController {

    private final MarsService marsService;

    public MarsController(MarsService marsService) {
        this.marsService = marsService;
    }

    @PostMapping("/ingest")
    public String ingest() {
        return marsService.ingestAndAnalyzeImages();
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> payload) {
        String question = payload.get("question");
        String answer = marsService.askQuestion(question);
        return Map.of("answer", answer);
    }
}