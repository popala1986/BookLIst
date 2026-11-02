package pl.booklist.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Looks up book covers in the Google Books API.
 * Without an API key Google allows only a small, shared number of requests per day,
 * so in production set {@code app.google-books.api-key} (env {@code GOOGLE_BOOKS_API_KEY}).
 */
@Service
public class CoverService {

    private static final Logger logger = LoggerFactory.getLogger(CoverService.class);

    public static final String DEFAULT_COVER_URL = "/images/default-cover.jpg";

    private static final String API_URL = "https://www.googleapis.com/books/v1/volumes";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public CoverService(RestTemplate restTemplate, @Value("${app.google-books.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey.trim();
    }

    /**
     * @return the cover URL of the best match, or {@link #DEFAULT_COVER_URL} when nothing is found
     */
    public String fetchCoverUrl(String title, String author) {
        try {
            JsonNode root = restTemplate.getForObject(buildUri(title, author), JsonNode.class);
            JsonNode items = root == null ? null : root.get("items");

            if (items != null && !items.isEmpty()) {
                JsonNode imageLinks = items.get(0).path("volumeInfo").path("imageLinks");
                if (imageLinks.has("thumbnail")) {
                    logger.info("Pobrano okładkę dla książki: {} - {}", title, author);
                    return imageLinks.get("thumbnail").asText().replaceFirst("^http://", "https://");
                }
            }
            logger.info("Brak okładki w Google Books dla: {} - {}", title, author);
        } catch (Exception e) {
            logger.warn("Nie udało się pobrać okładki dla książki: {} - {} ({}). Użyto domyślnej okładki.",
                    title, author, e.getMessage());
        }
        return DEFAULT_COVER_URL;
    }

    private URI buildUri(String title, String author) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(API_URL)
                .queryParam("q", title + " " + author)
                .queryParam("maxResults", 1);
        if (!apiKey.isEmpty()) {
            builder.queryParam("key", apiKey);
        }
        return builder.encode().build().toUri();
    }
}
