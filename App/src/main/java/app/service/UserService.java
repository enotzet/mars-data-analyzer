package app.service;

import app.model.ApiUsageRecord;
import app.model.UserProfile;
import app.repository.ApiUsageRepository;
import app.repository.UserProfileRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class UserService {

    private final UserProfileRepository userRepo;
    private final ApiUsageRepository usageRepo;

    public UserService(UserProfileRepository userRepo, ApiUsageRepository usageRepo) {
        this.userRepo = userRepo;
        this.usageRepo = usageRepo;
    }

    @PostConstruct
    public void seedDefaults() {
        if (userRepo.findByUsername("admin").isEmpty()) {
            userRepo.save(new UserProfile("admin", UserProfile.Role.ADMIN, 1000));
        }
        if (userRepo.findByUsername("default").isEmpty()) {
            userRepo.save(new UserProfile("default", UserProfile.Role.USER, 50));
        }
    }

    public UserProfile getOrCreateUser(String userId) {
        if (userId == null || userId.isBlank()) userId = "default";
        String finalUserId = userId;
        return userRepo.findByUsername(userId)
                .orElseGet(() -> userRepo.save(new UserProfile(finalUserId, UserProfile.Role.USER, 50)));
    }

    public boolean checkAndRecordUsage(String userId, String endpoint) {
        UserProfile user = getOrCreateUser(userId);
        Instant dayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long todayCount = usageRepo.countByUserIdAndTimestampAfter(userId, dayStart);
        if (todayCount >= user.getDailyLimit()) {
            return false;
        }
        usageRepo.save(new ApiUsageRecord(userId, endpoint));
        return true;
    }

    public long getTodayUsage(String userId) {
        Instant dayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
        return usageRepo.countByUserIdAndTimestampAfter(userId, dayStart);
    }
}
