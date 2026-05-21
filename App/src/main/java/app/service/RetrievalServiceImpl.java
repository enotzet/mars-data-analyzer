package app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RetrievalServiceImpl implements RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalServiceImpl.class);
    private final VectorStore vectorStore;

    public RetrievalServiceImpl(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<ScoredDocument> hybridSearch(String query, int topK) {
        List<Document> candidates = vectorStore.similaritySearch(
                SearchRequest.query(query).withTopK(topK * 3)
        );

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScoredDocument> scored = new ArrayList<>();
        Set<String> queryTerms = tokenize(query);

        for (int i = 0; i < candidates.size(); i++) {
            Document doc = candidates.get(i);
            double vectorScore = 1.0 - (i / (double) candidates.size());
            double keywordScore = computeKeywordOverlap(queryTerms, doc.getContent());
            double metadataBoost = computeMetadataBoost(doc);
            double combinedScore = (0.5 * vectorScore) + (0.35 * keywordScore) + (0.15 * metadataBoost);
            scored.add(new ScoredDocument(doc, combinedScore));
        }

        scored.sort(Comparator.comparingDouble(ScoredDocument::score).reversed());
        return scored.stream().limit(topK).collect(Collectors.toList());
    }

    @Override
    public List<ScoredDocument> baselineSearch(String query, int topK) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.query(query).withTopK(topK)
        );
        List<ScoredDocument> result = new ArrayList<>();
        for (int i = 0; i < docs.size(); i++) {
            result.add(new ScoredDocument(docs.get(i), 1.0 - (i / (double) Math.max(docs.size(), 1))));
        }
        return result;
    }

    @Override
    public boolean documentExistsByUrl(String imageUrl) {
        try {
            String safeUrl = imageUrl.replace("'", "\\'");
            return !vectorStore.similaritySearch(
                    SearchRequest.defaults()
                            .withQuery("check").withTopK(1)
                            .withFilterExpression("url == '" + safeUrl + "'")
            ).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void storeDocument(String content, Map<String, Object> metadata) {
        Document document = new Document(content, metadata);
        vectorStore.add(List.of(document));
    }

    private double computeKeywordOverlap(Set<String> queryTerms, String content) {
        if (queryTerms.isEmpty()) return 0.0;
        Set<String> contentTerms = tokenize(content);
        long matches = queryTerms.stream().filter(contentTerms::contains).count();
        return matches / (double) queryTerms.size();
    }

    private double computeMetadataBoost(Document doc) {
        double boost = 0.0;
        Map<String, Object> meta = doc.getMetadata();
        if (meta.containsKey("source")) {
            String source = meta.get("source").toString();
            if (source.contains("mars-rover")) boost += 0.5;
            if (source.contains("nasa")) boost += 0.3;
        }
        if (meta.containsKey("qualityScore")) {
            boost += 0.2 * Double.parseDouble(meta.get("qualityScore").toString());
        }
        return Math.min(boost, 1.0);
    }

    private Set<String> tokenize(String text) {
        if (text == null) return Collections.emptySet();
        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(w -> w.length() > 2)
                .collect(Collectors.toSet());
    }
}
