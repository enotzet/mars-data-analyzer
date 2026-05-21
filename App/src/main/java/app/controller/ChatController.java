package app.controller;

import app.dto.ChatRequest;
import app.dto.ChatResponse;
import app.service.MarsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mars")
public class ChatController {

    private final MarsService marsService;

    public ChatController(MarsService marsService) {
        this.marsService = marsService;
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
}
