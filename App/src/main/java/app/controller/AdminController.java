package app.controller;

import app.model.PromptTemplate;
import app.model.UserProfile;
import app.repository.UserProfileRepository;
import app.service.PromptService;
import app.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PromptService promptService;
    private final UserService userService;
    private final UserProfileRepository userProfileRepository;

    public AdminController(PromptService promptService, UserService userService, UserProfileRepository userProfileRepository) {
        this.promptService = promptService;
        this.userService = userService;
        this.userProfileRepository = userProfileRepository;
    }

    // --- Prompt Management ---

    @GetMapping("/prompts/{name}")
    public List<PromptTemplate> getPromptVersions(@PathVariable String name) {
        return promptService.getVersionHistory(name);
    }

    @GetMapping("/prompts/{name}/active")
    public PromptTemplate getActivePrompt(@PathVariable String name) {
        return promptService.getActivePrompt(name);
    }

    @PostMapping("/prompts/{name}")
    public PromptTemplate createPromptVersion(@PathVariable String name, @RequestBody Map<String, String> body) {
        String templateText = body.get("templateText");
        return promptService.createNewVersion(name, templateText);
    }

    // --- User Management ---

    @GetMapping("/users")
    public List<UserProfile> listUsers() {
        return userProfileRepository.findAll();
    }

    @GetMapping("/users/{username}/usage")
    public ResponseEntity<Map<String, Object>> getUserUsage(@PathVariable String username) {
        UserProfile user = userService.getOrCreateUser(username);
        long todayUsage = userService.getTodayUsage(username);
        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "role", user.getRole().name(),
                "dailyLimit", user.getDailyLimit(),
                "todayUsage", todayUsage
        ));
    }
}
