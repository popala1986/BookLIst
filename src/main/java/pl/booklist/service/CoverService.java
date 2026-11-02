package pl.booklist.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

/**
 * Looks up book covers: first in the Google Books API, then in Open Library.
 * Without an API key Google often refuses requests, so in production set
 * {@code app.google-books.api-key} (env {@code GOOGLE_BOOKS_API_KEY}).
 * Open Library needs no key and is used whenever Google finds nothing or fails.
 */
@Service
public class CoverService {

    private static final Logger logger = LoggerFactory.getLogger(CoverService.class);

    public static final String DEFAULT_COVER_URL = "/images/default-cover.jpg";

    private static final String GOOGLE_API_URL = "https://www.googleapis.com/books/v1/volumes";
    private static final String OPEN_LIBRARY_SEARCH_URL = "https://openlibrary.org/search.json";
    private static final String OPEN_LIBRARY_COVER_URL = "https://covers.openlibrary.org/b/id/%d-L.jpg";

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
        return fromGoogle(title, author)
                .or(() -> fromOpenLibrary(title, author))
                .orElseGet(() -> {
                    logger.info("Brak okładki dla: {} - {}. Użyto domyślnej okładki.", title, author);
                    return DEFAULT_COVER_URL;
                });
    }

    private Optional<String> fromGoogle(String title, String author) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(GOOGLE_API_URL)
                .queryParam("q", title + " " + author)
                .queryParam("maxResults", 1);
        if (!apiKey.isEmpty()) {
            builder.queryParam("key", apiKey);
        }
        try {
            JsonNode items = get(builder.encode().build().toUri()).path("items");
            JsonNode thumbnail = items.path(0).path("volumeInfo").path("imageLinks").path("thumbnail");
            if (thumbnail.isTextual()) {
                logger.info("Okładka z Google Books dla: {} - {}", title, author);
                return Optional.of(thumbnail.asText().replaceFirst("^http://", "https://"));
            }
        } catch (Exception e) {
            logger.warn("Google Books nie odpowiedział dla: {} - {} ({})", title, author, shortMessage(e));
        }
        return Optional.empty();
    }

    private Optional<String> fromOpenLibrary(String title, String author) {
        URI uri = UriComponentsBuilder.fromUriString(OPEN_LIBRARY_SEARCH_URL)
                .queryParam("q", title + " " + author)
                .queryParam("fields", "cover_i")
                .queryParam("limit", 5)
                .encode().build().toUri();
        try {
            for (JsonNode doc : get(uri).path("docs")) {
                JsonNode coverId = doc.path("cover_i");
                if (coverId.canConvertToLong() && coverId.asLong() > 0) {
                    logger.info("Okładka z Open Library dla: {} - {}", title, author);
                    return Optional.of(OPEN_LIBRARY_COVER_URL.formatted(coverId.asLong()));
                }
            }
        } catch (Exception e) {
            logger.warn("Open Library nie odpowiedziało dla: {} - {} ({})", title, author, shortMessage(e));
        }
        return Optional.empty();
    }

    private JsonNode get(URI uri) {
        JsonNode root = restTemplate.getForObject(uri, JsonNode.class);
        return root == null ? MissingNode.getInstance() : root;
    }

    /** First line of the error, without the whole response body that RestTemplate puts in the message. */
    private static String shortMessage(Exception e) {
        String message = String.valueOf(e.getMessage());
        int cut = message.indexOf(": \"");
        return cut > 0 ? message.substring(0, cut) : message;
    }
}
