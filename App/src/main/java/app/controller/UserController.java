package app.controller;

import app.model.UserProfile;
import app.repository.UserProfileRepository;
import app.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserService userService;
    private final UserProfileRepository userProfileRepository;

    public UserController(UserService userService, UserProfileRepository userProfileRepository) {
        this.userService = userService;
        this.userProfileRepository = userProfileRepository;
    }

    @GetMapping
    public List<UserProfile> listUsers() {
        return userProfileRepository.findAll();
    }

    @GetMapping("/{username}/usage")
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
