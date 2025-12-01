package app.service;

import app.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class MarsService {

    private final RestTemplate restTemplate;

    // injecting from application.properties
    @Value("${openai.api.key}")
    private String openAiKey;

    public MarsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public AnalysisResult analyzeMarsData() {
        String nasaData = fetchNasaData();

        String gptAnalysis = analyzeWithGpt(nasaData);

        return new AnalysisResult(nasaData, gptAnalysis);
    }

    private String fetchNasaData() {
        String url = "https://pds-imaging.jpl.nasa.gov/solr/pds_archives/search";

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host("pds-imaging.jpl.nasa.gov")
                .path("/solr/pds_archives/search")
                .queryParam("mission", "Mars Science Laboratory")
                .queryParam("target", "Mars")
                .queryParam("rows", "3")
                .queryParam("wt", "json");

        String finalUrl = builder.toUriString();

        return restTemplate.getForObject(finalUrl, String.class);
    }

    private String analyzeWithGpt(String data) {
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiKey);

        if (data.length() > 5000) {
            data = data.substring(0, 5000) + "... [truncated]";
        }

        var systemMsg = new GptRequest.Message("system",
                "You are a planetary science assistant. Analyze this NASA JSON data.");
        var userMsg = new GptRequest.Message("user", "Data: " + data);

        var requestBody = new GptRequest("gpt-4o-mini", List.of(systemMsg, userMsg));

        HttpEntity<GptRequest> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<GptResponse> response = restTemplate.postForEntity(url, entity, GptResponse.class);

            if (response.getBody() != null && !response.getBody().choices().isEmpty()) {
                return response.getBody().choices().get(0).message().content();
            }
        } catch (Exception e) {
            return "Error analyzing data: " + e.getMessage();
        }
        return "No analysis returned.";
    }
}