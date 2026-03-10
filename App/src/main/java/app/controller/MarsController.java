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
    public Map<String, String> chat(@RequestBody Map<String, Object> payload) {
        String question = (String) payload.get("question");
        String sessionId = (String) payload.get("sessionId");
        boolean ragEnabled = (Boolean) payload.get("ragEnabled");

        String answer = marsService.askQuestion(question, sessionId, ragEnabled);
        return Map.of("answer", answer);
    }
}