package app.controller;


import app.dto.AnalysisResult;
import app.service.MarsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mars")
public class MarsController {

    private final MarsService marsService;

    public MarsController(MarsService marsService) {
        this.marsService = marsService;
    }

    @GetMapping("/analyze")
    public AnalysisResult analyze() {
        return marsService.analyzeMarsData();
    }
}
