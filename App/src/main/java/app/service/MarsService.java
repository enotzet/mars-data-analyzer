package app.service;

import app.dto.ChatResponse;
import app.model.ChatLog;
import app.model.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MarsService {

    private static final Logger log = LoggerFactory.getLogger(MarsService.class);

    private final ChatModel chatModel;
    private final RetrievalService retrievalService;
    private final PromptService promptService;
    private final ProvenanceService provenanceService;
    private final UserService userService;

    private static final String MODEL_NAME = "gpt-4o-mini";
    private static final double TEMPERATURE = 0.7;
    private static final int MAX_TOKENS = 1500;

    public MarsService(
            ChatModel chatModel,
            RetrievalService retrievalService,
            PromptService promptService,
            ProvenanceService provenanceService,
            UserService userService) {
        this.chatModel = chatModel;
        this.retrievalService = retrievalService;
        this.promptService = promptService;
        this.provenanceService = provenanceService;
        this.userService = userService;
    }

    public ChatResponse askQuestionWithEvidence(String userQuery, String sessionId, boolean ragEnabled, String userId) {
        long startTime = System.currentTimeMillis();
        String jobId = UUID.randomUUID().toString();

        // Rate limit check
        if (!userService.checkAndRecordUsage(userId != null ? userId : "default", "/api/mars/chat")) {
            return new ChatResponse("Daily API limit reached. Please try again tomorrow.", jobId, null, List.of());
        }

        String systemText;
        String context = "";
        List<ChatResponse.SourceEvidence> sourceEvidences = new ArrayList<>();
        List<ChatLog.SourceReference> sourceRefs = new ArrayList<>();
        PromptTemplate activePrompt;

        if (ragEnabled) {
            List<RetrievalService.ScoredDocument> scoredDocs = retrievalService.hybridSearch(userQuery, 3);

            if (!scoredDocs.isEmpty()) {
                StringBuilder contextBuilder = new StringBuilder();
                for (int i = 0; i < scoredDocs.size(); i++) {
                    var sd = scoredDocs.get(i);
                    var doc = sd.document();
                    String url = doc.getMetadata().getOrDefault("url", "").toString();
                    String source = doc.getMetadata().getOrDefault("source", "unknown").toString();
                    String snippet = doc.getContent().substring(0, Math.min(300, doc.getContent().length()));

                    contextBuilder.append("[Source ").append(i + 1).append("] ")
                            .append("Description: ").append(doc.getContent())
                            .append("\nImage URL: ").append(url)
                            .append("\n---\n");

                    sourceEvidences.add(new ChatResponse.SourceEvidence(url, source, snippet, sd.score()));
                    sourceRefs.add(new ChatLog.SourceReference(url, source, snippet, sd.score()));
                }
                context = contextBuilder.toString();
            }
        }

        if (ragEnabled && !context.isEmpty()) {
            activePrompt = promptService.getActivePrompt(PromptService.RAG_CHAT);
        } else {
            activePrompt = promptService.getActivePrompt(PromptService.GENERAL_CHAT);
        }

        systemText = activePrompt.getTemplateText();
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemText);
        Prompt prompt = systemPromptTemplate.create(Map.of("context", context));

        var finalPrompt = new Prompt(
                List.of(prompt.getInstructions().get(0), new UserMessage(userQuery)),
                OpenAiChatOptions.builder()
                        .withTemperature((float) TEMPERATURE)
                        .withMaxTokens(MAX_TOKENS)
                        .build());

        String answer = chatModel.call(finalPrompt).getResult().getOutput().getContent();
        long latencyMs = System.currentTimeMillis() - startTime;

        // Log provenance to Elasticsearch
        provenanceService.logInteraction(
                jobId, sessionId, userId, ragEnabled,
                userQuery, answer, context,
                activePrompt.getId(), MODEL_NAME, TEMPERATURE, MAX_TOKENS,
                latencyMs, sourceRefs
        );

        return new ChatResponse(answer, jobId, activePrompt.getId(), sourceEvidences);
    }

    /** Simplified method for backward compatibility and benchmarking. */
    public String askQuestion(String userQuery, String sessionId, boolean ragEnabled) {
        ChatResponse response = askQuestionWithEvidence(userQuery, sessionId, ragEnabled, "default");
        return response.answer();
    }
}
