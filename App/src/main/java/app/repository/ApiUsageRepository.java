package app.repository;

import app.model.ApiUsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface ApiUsageRepository extends JpaRepository<ApiUsageRecord, String> {
    long countByUserIdAndTimestampAfter(String userId, Instant after);
}
