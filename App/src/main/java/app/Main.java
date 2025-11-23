package app;

import javax.net.ssl.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;

public class Main {

    private static final String OPENAI_API_KEY = "CHANGE_WHILE_TESTING";

    public static void main(String[] args) {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");

            HttpClient client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();

            System.out.println("1. Getting data from NASA...");
            String nasaUrl = "https://pds-imaging.jpl.nasa.gov/solr/pds_archives/search";
            String mission = URLEncoder.encode("Mars Science Laboratory", StandardCharsets.UTF_8);
            String target = URLEncoder.encode("Mars", StandardCharsets.UTF_8);
            String fullNasaUrl = String.format("%s?mission=%s&target=%s&rows=3&wt=json", nasaUrl, mission, target);

            HttpRequest nasaRequest = HttpRequest.newBuilder()
                    .uri(URI.create(fullNasaUrl))
                    .GET()
                    .build();

            HttpResponse<String> nasaResponse = client.send(nasaRequest, HttpResponse.BodyHandlers.ofString());

            if (nasaResponse.statusCode() == 200) {
                String nasaJsonData = nasaResponse.body();
                System.out.println("   NASA Data received (" + nasaJsonData.length() + " chars).");

                analyzeWithGPT(client, nasaJsonData);

            } else {
                System.out.println("NASA Request failed: " + nasaResponse.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void analyzeWithGPT(HttpClient client, String dataToAnalyze) {
        System.out.println("\n2. Sending data to GPT-4o-mini for analysis...");

        try {
            String escapedData = dataToAnalyze
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")
                    .replace("\r", " ");

            String jsonBody = String.format(
                    "{" +
                            "  \"model\": \"gpt-4o-mini\"," +
                            "  \"messages\": [" +
                            "    {\"role\": \"system\", \"content\": \"You are a planetary science assistant. Analyze the provided JSON data from the Mars Science Laboratory. Briefly describe what kind of images or data was found, mentioning instruments and dates if possible.\"}," +
                            "    {\"role\": \"user\", \"content\": \"Analyze this data: %s\"}" +
                            "  ]" +
                            "}", escapedData);

            HttpRequest gptRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + OPENAI_API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> gptResponse = client.send(gptRequest, HttpResponse.BodyHandlers.ofString());

            if (gptResponse.statusCode() == 200) {
                System.out.println("\n--- GPT ANALYSIS RESULT ---");

                String content = extractContentSimple(gptResponse.body());
                System.out.println(content);

                System.out.println("---------------------------");

            } else {
                System.out.println("GPT Request failed: " + gptResponse.statusCode());
                System.out.println("Response: " + gptResponse.body());
            }

        } catch (Exception e) {
            System.out.println("Error calling GPT: " + e.getMessage());
        }
    }

    private static String extractContentSimple(String json) {
        String marker = "\"content\": \"";
        int start = json.indexOf( marker );
        if ( start == -1 ) {
            return "Could not find content in response. Raw JSON:\n" + json;
        }

        start += marker.length();
        StringBuilder sb = new StringBuilder();
        boolean isEscaped = false;

        for ( int i = start; i < json.length(); i++ ) {
            char c = json.charAt( i );

            if ( isEscaped ) {
                sb.append( c );
                isEscaped = false;
            }
            else {
                if ( c == '\\' ) {
                    isEscaped = true;
                }
                else if ( c == '"' ) {
                    break;
                }
                else {
                    sb.append( c );
                }
            }
        }

        return sb.toString().replace( "\\n", "\n" ).replace( "\\\"", "\"" );
    }
}