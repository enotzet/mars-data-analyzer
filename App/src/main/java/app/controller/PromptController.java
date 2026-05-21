package app.controller;

import app.model.PromptTemplate;
import app.service.PromptService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/prompts")
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping("/{name}")
    public List<PromptTemplate> getPromptVersions(@PathVariable String name) {
        return promptService.getVersionHistory(name);
    }

    @GetMapping("/{name}/active")
    public PromptTemplate getActivePrompt(@PathVariable String name) {
        return promptService.getActivePrompt(name);
    }

    @PostMapping("/{name}")
    public PromptTemplate createPromptVersion(@PathVariable String name, @RequestBody Map<String, String> body) {
        String templateText = body.get("templateText");
        return promptService.createNewVersion(name, templateText);
    }
}
