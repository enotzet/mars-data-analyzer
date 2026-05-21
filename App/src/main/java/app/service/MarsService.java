package app.service;

import app.dto.ChatResponse;

public interface MarsService {

    /** Production chat path: hybrid retrieval + rag-chat prompt + provenance + quota. */
    ChatResponse askQuestionWithEvidence(String userQuery, String sessionId, boolean ragEnabled, String userId);

    /** Convenience for benchmark: returns just the answer string, runs through hybrid retrieval if rag enabled. */
    String askQuestion(String userQuery, String sessionId, boolean ragEnabled);

    /** Benchmark-only path: runs the user query under one of the four evaluation configurations and returns the generated answer. */
    BenchmarkAnswer askInMode(String userQuery, BenchmarkMode mode);

    /** The four evaluation configurations used by BenchmarkService. */
    enum BenchmarkMode {
        ZERO_SHOT,        // no system prompt, no retrieval
        SYSTEM_PROMPT,    // general-chat system prompt, no retrieval
        RAG_BASELINE,     // pure dense retrieval + rag-chat prompt
        RAG_RERANKED      // hybrid retrieval (rerank) + rag-chat prompt
    }

    record BenchmarkAnswer(String answer, int sourcesUsed, double avgRetrievalScore) {}
}
