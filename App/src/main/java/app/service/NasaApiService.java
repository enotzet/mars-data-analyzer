package app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
public class NasaApiService {

    private static final Logger log = LoggerFactory.getLogger(NasaApiService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${nasa.api.key:DEMO_KEY}")
    private String nasaApiKey;

    public NasaApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** Fetch image URLs from NASA Image and Video Library. */
    public List<ImageRecord> fetchNasaImageLibrary() {
        String url = UriComponentsBuilder.fromHttpUrl("https://images-api.nasa.gov/search")
                .queryParam("q", "Mars Curiosity Surface")
                .queryParam("media_type", "image")
                .queryParam("year_start", "2016")
                .queryParam("page", "1")
                .build().toUriString();

        String json = restTemplate.getForObject(url, String.class);
        List<ImageRecord> records = new ArrayList<>();
        try {
            JsonNode items = objectMapper.readTree(json).path("collection").path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    JsonNode data = item.path("data");
                    String title = data.isArray() && !data.isEmpty() ? data.get(0).path("title").asText("") : "";
                    String description = data.isArray() && !data.isEmpty() ? data.get(0).path("description").asText("") : "";
                    String nasaId = data.isArray() && !data.isEmpty() ? data.get(0).path("nasa_id").asText("") : "";
                    JsonNode links = item.path("links");
                    if (links.isArray() && !links.isEmpty()) {
                        String href = links.get(0).path("href").asText();
                        if (href != null && !href.contains(".tif")) {
                            records.add(new ImageRecord(href, "nasa-image-library", title, description, nasaId));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse NASA Image Library response: {}", e.getMessage());
        }
        return records;
    }

    /** Fetch Mars Rover photos from the Mars Rover Photos API. */
    public List<ImageRecord> fetchMarsRoverPhotos() {
        String url = UriComponentsBuilder.fromHttpUrl("https://api.nasa.gov/mars-photos/api/v1/rovers/curiosity/photos")
                .queryParam("sol", "1000")
                .queryParam("page", "1")
                .queryParam("api_key", nasaApiKey)
                .build().toUriString();

        List<ImageRecord> records = new ArrayList<>();
        try {
            String json = restTemplate.getForObject(url, String.class);
            JsonNode photos = objectMapper.readTree(json).path("photos");
            if (photos.isArray()) {
                for (JsonNode photo : photos) {
                    String imgSrc = photo.path("img_src").asText();
                    String camera = photo.path("camera").path("full_name").asText("");
                    String earthDate = photo.path("earth_date").asText("");
                    int photoId = photo.path("id").asInt();
                    records.add(new ImageRecord(
                        imgSrc, "mars-rover-photos",
                        "Curiosity " + camera + " " + earthDate,
                        "Mars Rover Curiosity photo taken by " + camera + " on " + earthDate,
                        "rover-" + photoId
                    ));
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch Mars Rover photos: {}", e.getMessage());
        }
        return records;
    }

    /** Download image bytes from a URL. Returns null on failure. */
    public byte[] downloadImage(String imageUrl) {
        try {
            var uri = java.net.URI.create(imageUrl.replace(" ", "%20"));
            return restTemplate.getForObject(uri, byte[].class);
        } catch (Exception e) {
            log.error("Failed to download image {}: {}", imageUrl, e.getMessage());
            return null;
        }
    }

    public record ImageRecord(
        String imageUrl,
        String source,
        String title,
        String description,
        String externalId
    ) {}
}
