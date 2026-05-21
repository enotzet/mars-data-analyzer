package app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
public class InSightWeatherAdapterImpl implements InSightWeatherAdapter {

    private static final Logger log = LoggerFactory.getLogger(InSightWeatherAdapterImpl.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${nasa.api.key:DEMO_KEY}")
    private String nasaApiKey;

    public InSightWeatherAdapterImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<WeatherRecord> fetchWeather() {
        String url = UriComponentsBuilder.fromHttpUrl("https://api.nasa.gov/insight_weather/")
                .queryParam("api_key", nasaApiKey)
                .queryParam("feedtype", "json")
                .queryParam("ver", "1.0")
                .build().toUriString();

        List<WeatherRecord> records = new ArrayList<>();
        try {
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);

            if (root.has("error")) {
                log.warn("InSight weather API error: {}", root.path("error").path("message").asText());
                return records;
            }

            JsonNode solKeys = root.path("sol_keys");
            if (!solKeys.isArray()) {
                log.warn("InSight response missing sol_keys array");
                return records;
            }

            for (JsonNode solNode : solKeys) {
                String sol = solNode.asText();
                JsonNode solData = root.path(sol);
                if (solData.isMissingNode() || !solData.isObject()) continue;

                records.add(parseSol(sol, solData));
            }
        } catch (Exception e) {
            log.error("Failed to fetch InSight weather: {}", e.getMessage());
        }
        return records;
    }

    private WeatherRecord parseSol(String sol, JsonNode solData) {
        JsonNode at = solData.path("AT");
        JsonNode hws = solData.path("HWS");
        JsonNode pre = solData.path("PRE");
        JsonNode wd = solData.path("WD").path("most_common");

        return new WeatherRecord(
                sol,
                solData.path("Season").asText(null),
                solData.path("First_UTC").asText(null),
                solData.path("Last_UTC").asText(null),
                doubleOrNull(at, "av"),
                doubleOrNull(at, "mn"),
                doubleOrNull(at, "mx"),
                doubleOrNull(hws, "av"),
                doubleOrNull(hws, "mx"),
                doubleOrNull(pre, "av"),
                doubleOrNull(pre, "mn"),
                doubleOrNull(pre, "mx"),
                wd.isMissingNode() ? null : wd.path("compass_point").asText(null)
        );
    }

    private Double doubleOrNull(JsonNode parent, String field) {
        JsonNode v = parent.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asDouble();
    }
}
