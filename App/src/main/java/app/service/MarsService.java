package app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.Media; // Media переехал сюда
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MarsService {

    private final RestTemplate restTemplate;
    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    static Logger log = LoggerFactory.getLogger( MarsService.class );


    public MarsService(RestTemplate restTemplate, ChatModel chatModel, VectorStore vectorStore) {
        this.restTemplate = restTemplate;
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.objectMapper = new ObjectMapper();
    }

    public String ingestAndAnalyzeImages() {
        String nasaJson = fetchNasaData();
        List<String> imageUrls = extractImageUrls(nasaJson);

        if (imageUrls.isEmpty()) return "No images found.";

        int count = 0;

        for (String imageUrl : imageUrls) {
            if (count >= 3) break;

            try {
                if (checkImageExists(imageUrl)) {
                    log.info( "Skipping existing: {}", imageUrl );
                    continue;
                }

                log.info( "Downloading: {}", imageUrl );
                URI uri = URI.create(imageUrl.replace(" ", "%20"));
                byte[] imageBytes = restTemplate.getForObject(uri, byte[].class);

                if (imageBytes == null || imageBytes.length < 2048) {
                    log.warn("Skipping small file.");
                    continue;
                }

                log.info( "Analyzing ({} bytes)...", imageBytes.length );

                var userMessage = new UserMessage("You are an astronomical expert with extensive knowledge of Mars; describe in detail everything you see in this image of the Martian surface.",
                        List.of(new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(imageBytes))));

                Prompt prompt = new Prompt(userMessage,
                        OpenAiChatOptions.builder()
                                .withMaxTokens(500)
                                .build());

                String description = chatModel.call(prompt).getResult().getOutput().getContent();
                System.out.println("Analysis: " + description.substring(0, Math.min(50, description.length())) + "...");

                Document document = new Document(description, Map.of("url", imageUrl, "source", "nasa-api"));
                vectorStore.add(List.of(document));
                count++;

            } catch (Exception e) {
                log.error( "Error: {}", e.getMessage() );
            }
        }
        return "Ingestion complete. Analyzed: " + count;
    }

    public String askQuestion(String userQuery) {
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.query(userQuery).withTopK(3)
        );

        String context = similarDocuments.stream()
                .map(d -> "Description: " + d.getContent() + "\nImage URL: " + d.getMetadata().get("url"))
                .collect(Collectors.joining("\n---\n"));

        if (context.isEmpty()) return "No data found. Ingest images first.";

        String systemText = """
                You are a Mars expert. Use the provided Context to answer the user question.
                Context:
                {context}
                """;

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemText);
        Prompt prompt = systemPromptTemplate.create(Map.of("context", context));

        var finalPrompt = new Prompt(List.of(prompt.getInstructions().get(0), new UserMessage(userQuery)),
                OpenAiChatOptions.builder().build());

        return chatModel.call(finalPrompt).getResult().getOutput().getContent();
    }

    private boolean checkImageExists(String imageUrl) {
        try {
            String safeUrl = imageUrl.replace("'", "\\'");
            return !vectorStore.similaritySearch(
                    SearchRequest.defaults()
                            .withQuery("check").withTopK(1)
                            .withFilterExpression("url == '" + safeUrl + "'")
            ).isEmpty();
        } catch (Exception e) { return false; }
    }

    private String fetchNasaData() {
        String url = "https://images-api.nasa.gov/search";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("q", "Mars Curiosity Surface")
                .queryParam("media_type", "image")
                .queryParam("year_start", "2016")
                .queryParam("page", "1");
        return restTemplate.getForObject(builder.build().toUri(), String.class);
    }

    private List<String> extractImageUrls(String json) {
        List<String> urls = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("collection").path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    JsonNode links = item.path("links");
                    if (links.isArray() && !links.isEmpty()) {
                        String href = links.get(0).path("href").asText();
                        if (href != null && !href.contains(".tif")) urls.add(href);
                    }
                }
            }
        } catch (Exception e) { }
        return urls;
    }
}