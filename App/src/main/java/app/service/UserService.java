package app.service;

import app.model.UserProfile;

public interface UserService {

    UserProfile getOrCreateUser(String userId);

    boolean checkAndRecordUsage(String userId, String endpoint);

    long getTodayUsage(String userId);
}
