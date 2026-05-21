package app.repository;

import app.model.ChatLog;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ChatLogRepository extends ElasticsearchRepository<ChatLog, String> {

    List<ChatLog> findByUserIdOrderByTimestampDesc(String userId);

    List<ChatLog> findBySessionIdOrderByTimestampAsc(String sessionId);
}
