package pl.booklist.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

class CoverServiceTest {

    private final RestTemplate restTemplate = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

    @Test
    @DisplayName("returns the https thumbnail of the first result and sends the API key")
    void returnsThumbnail() {
        server.expect(requestTo(startsWith("https://www.googleapis.com/books/v1/volumes")))
                .andExpect(queryParam("key", "secret"))
                .andRespond(withSuccess("""
                        {"items":[{"volumeInfo":{"imageLinks":{"thumbnail":"http://books.google.com/x.jpg"}}}]}
                        """, MediaType.APPLICATION_JSON));

        String url = new CoverService(restTemplate, "secret").fetchCoverUrl("Wiedźmin", "Sapkowski");

        assertThat(url).isEqualTo("https://books.google.com/x.jpg");
        server.verify();
    }

    @Test
    @DisplayName("falls back to the default cover when the API fails")
    void fallsBackOnError() {
        server.expect(requestTo(startsWith("https://www.googleapis.com/books/v1/volumes")))
                .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));

        assertThat(new CoverService(restTemplate, "").fetchCoverUrl("Lód", "Dukaj"))
                .isEqualTo(CoverService.DEFAULT_COVER_URL);
    }

    @Test
    @DisplayName("falls back to the default cover when nothing is found")
    void fallsBackWhenNoResults() {
        server.expect(requestTo(startsWith("https://www.googleapis.com/books/v1/volumes")))
                .andRespond(withSuccess("{\"totalItems\":0}", MediaType.APPLICATION_JSON));

        assertThat(new CoverService(restTemplate, "").fetchCoverUrl("Nieistniejąca", "Nikt"))
                .isEqualTo(CoverService.DEFAULT_COVER_URL);
    }
}
