package app.service;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

public interface RetrievalService {

    List<ScoredDocument> hybridSearch(String query, int topK);

    List<ScoredDocument> baselineSearch(String query, int topK);

    boolean documentExistsByUrl(String imageUrl);

    void storeDocument(String content, Map<String, Object> metadata);

    record ScoredDocument(Document document, double score) {}
}
