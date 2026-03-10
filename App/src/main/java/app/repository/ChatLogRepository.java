package app.repository;

import app.model.ChatLog;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ChatLogRepository extends ElasticsearchRepository<ChatLog, String> {
}