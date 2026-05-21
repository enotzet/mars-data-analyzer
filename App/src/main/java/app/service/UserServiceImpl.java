package app.service;

import app.model.ApiUsageRecord;
import app.model.UserProfile;
import app.repository.ApiUsageRepository;
import app.repository.UserProfileRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserProfileRepository userRepo;
    private final ApiUsageRepository usageRepo;
    private final TransactionTemplate createUserTx;

    public UserServiceImpl(UserProfileRepository userRepo,
                           ApiUsageRepository usageRepo,
                           PlatformTransactionManager txManager) {
        this.userRepo = userRepo;
        this.usageRepo = usageRepo;
        this.createUserTx = new TransactionTemplate(txManager);
        this.createUserTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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

    @Override
    @Transactional
    public UserProfile getOrCreateUser(String userId) {
        if (userId == null || userId.isBlank()) userId = "default";
        final String username = userId;

        Optional<UserProfile> existing = userRepo.findByUsername(username);
        if (existing.isPresent()) return existing.get();

        try {
            return createUserTx.execute(status ->
                    userRepo.saveAndFlush(new UserProfile(username, UserProfile.Role.USER, 50))
            );
        } catch (DataIntegrityViolationException e) {
            return userRepo.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException(
                            "Username '" + username + "' conflicted on insert but not found on retry", e));
        }
    }

    @Override
    @Transactional
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

    @Override
    public long getTodayUsage(String userId) {
        Instant dayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
        return usageRepo.countByUserIdAndTimestampAfter(userId, dayStart);
    }
}
