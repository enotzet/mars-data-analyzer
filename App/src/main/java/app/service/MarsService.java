package app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Media;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MarsService {

    private final RestTemplate restTemplate;
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;

    public MarsService(RestTemplate restTemplate, ChatClient chatClient, VectorStore vectorStore) {
        this.restTemplate = restTemplate;
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.objectMapper = new ObjectMapper();
    }

    public String ingestAndAnalyzeImages() {
        String nasaJson = fetchNasaData();
        List<String> imageUrls = extractImageUrls(nasaJson);

        int count = 0;
        int skipped = 0;

        System.out.println("Found " + imageUrls.size() + " images. Starting processing...");

        for (String imageUrl : imageUrls) {
            try {

                boolean exists = checkImageExists(imageUrl);

                if (exists) {
                    System.out.println("Image already exists in DB, skipping: " + imageUrl);
                    skipped++;
                    continue;
                }

                System.out.println("Analyzing new image: " + imageUrl);


                var userMessage = new UserMessage("Describe this image by its URL, in +é words regarding geological features.",
                        List.of(new Media(MimeTypeUtils.IMAGE_JPEG, new UrlResource(imageUrl))));

                String description = chatClient.call(new Prompt(userMessage)).getResult().getOutput().getContent();

                Document document = new Document(description, Map.of("url", imageUrl, "source", "nasa"));
                vectorStore.add(List.of(document));

                count++;

            } catch (Exception e) {
                System.err.println("Error processing image: " + imageUrl + " -> " + e.getMessage());
            }
        }
        return "Ingestion complete. Analyzed: " + count + ", Skipped (already existed): " + skipped;
    }

    public String askQuestion(String userQuery) {
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.query(userQuery).withTopK(3) // Берем топ-3 похожих
        );

        String context = similarDocuments.stream()
                .map(d -> "Description: " + d.getContent() + "\nImage URL: " + d.getMetadata().get("url"))
                .collect(Collectors.joining("\n---\n"));

        if (context.isEmpty()) {
            return "I haven't analyzed any images yet, or nothing relevant was found.";
        }

        String systemText = """
                You are a Mars expert. Use the provided Context to answer the user question.
                If the answer contains a reference to an image, include the Image URL in your answer.
                
                Context:
                {context}
                """;

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemText);
        Prompt prompt = systemPromptTemplate.create(Map.of("context", context));

        var finalPrompt = new Prompt(List.of(prompt.getInstructions().get(0), new UserMessage(userQuery)));

        return chatClient.call(finalPrompt).getResult().getOutput().getContent();
    }


    private boolean checkImageExists(String imageUrl) {
        try {

            String filterExpression = "url == '" + imageUrl + "'";

            List<Document> existing = vectorStore.similaritySearch(
                    SearchRequest.defaults()
                            .withQuery("check")
                            .withTopK(1)
                            .withFilterExpression(filterExpression)
            );
            return !existing.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private String fetchNasaData() {
        String url = "https://pds-imaging.jpl.nasa.gov/solr/pds_archives/search";

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host("pds-imaging.jpl.nasa.gov")
                .path("/solr/pds_archives/search")
                .queryParam("mission", "Mars Science Laboratory")
                .queryParam("target", "Mars")
                .queryParam("rows", "5") // limit of rows
                .queryParam("wt", "json");
        return restTemplate.getForObject(builder.toUriString(), String.class);
    }

    private List<String> extractImageUrls(String json) {
        List<String> urls = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode docs = root.path("response").path("docs");
            if (docs.isArray()) {
                for (JsonNode doc : docs) {
                    if (doc.has("ATLAS_THUMBNAIL_URL")) {
                        urls.add(doc.get("ATLAS_THUMBNAIL_URL").asText());
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return urls;
    }
}