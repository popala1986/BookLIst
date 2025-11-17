package pl.booklist.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class CoverService {

    private static final Logger logger = LoggerFactory.getLogger(CoverService.class);

    private static final String DEFAULT_COVER_URL = "/images/default-cover.jpg";

    private final RestTemplate restTemplate;

    public CoverService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String fetchCoverUrl(String title, String author) {

        String query = URLEncoder.encode(title + " " + author, StandardCharsets.UTF_8);
        String apiUrl = "https://www.googleapis.com/books/v1/volumes?q=" + query + "&maxResults=1";

        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(apiUrl, JsonNode.class);
            JsonNode root = response.getBody();
            JsonNode items = root.get("items");

            if (items != null && items.size() > 0) {
                JsonNode volumeInfo = items.get(0).get("volumeInfo");

                if (volumeInfo != null) {
                    JsonNode imageLinks = volumeInfo.get("imageLinks");

                    if (imageLinks != null && imageLinks.has("thumbnail")) {
                        String thumbnailUrl = imageLinks.get("thumbnail").asText();
                        logger.info("Pobrano okładkę dla książki: {} - {}", title, author);
                        return thumbnailUrl;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Nie udało się pobrać okładki dla książki: {} - {}. Użyto domyślnej okładki.", title, author, e);
        }

        return DEFAULT_COVER_URL;
    }
}