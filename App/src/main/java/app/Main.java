package app;

import javax.net.ssl.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;

public class Main {
    public static void main(String[] args) {
        try {

            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");

            String baseUrl = "https://pds-imaging.jpl.nasa.gov/solr/pds_archives/search";
            String missionQuery = "Mars Science Laboratory";
            String targetQuery = "Mars";

            String encodedMission = URLEncoder.encode(missionQuery, StandardCharsets.UTF_8);
            String encodedTarget = URLEncoder.encode(targetQuery, StandardCharsets.UTF_8);

            String fullUrl = String.format("%s?mission=%s&target=%s&rows=5",
                    baseUrl, encodedMission, encodedTarget);

            System.out.println("Sending request to: " + fullUrl);

            HttpClient client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("\nSuccess! Server response:");
                System.out.println(response.body());
            } else {
                System.out.println("Request failed. Status code: " + response.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("An error occurred during the request.");
        }
    }
}