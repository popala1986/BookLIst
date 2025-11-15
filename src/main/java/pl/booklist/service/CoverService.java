package pl.booklist.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service responsible for fetching book cover images from external sources.
 */
@Service
public class CoverService {

    private static final Logger logger = LoggerFactory.getLogger(CoverService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Fetches the cover image URL for a book using Google Books API.
     *
     * @param title  the title of the book
     * @param author the author of the book
     * @return the URL of the book's cover image, or a default image if not found
     */
    public String fetchCoverUrl(String title, String author) {
        String query = URLEncoder.encode("intitle:" + title + "+inauthor:" + author, StandardCharsets.UTF_8);
        String apiUrl = "https://www.googleapis.com/books/v1/volumes?q=" + query;

        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(apiUrl, JsonNode.class);
            JsonNode items = response.getBody().get("items");

            if (items != null && items.size() > 0) {
                JsonNode imageLinks = items.get(0).get("volumeInfo").get("imageLinks");
                if (imageLinks != null && imageLinks.has("thumbnail")) {
                    String thumbnailUrl = imageLinks.get("thumbnail").asText();
                    logger.info("Pobrano okładkę dla książki: {} - {}", title, author);
                    return thumbnailUrl;
                }
            }
        } catch (Exception e) {
            logger.warn("Nie udało się pobrać okładki dla książki: {} - {}. Użyto domyślnej okładki.", title, author, e);
        }

        return "/images/default-cover.jpg";
    }
}
