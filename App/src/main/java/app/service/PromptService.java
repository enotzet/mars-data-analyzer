package app.service;

import app.model.PromptTemplate;

import java.util.List;

public interface PromptService {

    String IMAGE_ANALYSIS = "image-analysis";
    String RAG_CHAT = "rag-chat";
    String GENERAL_CHAT = "general-chat";

    PromptTemplate getActivePrompt(String name);

    PromptTemplate createNewVersion(String name, String templateText);

    List<PromptTemplate> getVersionHistory(String name);
}
