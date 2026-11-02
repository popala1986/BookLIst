package pl.booklist.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("anonymous user is redirected to the login page")
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("API rejects requests without credentials")
    void apiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/books/showAllBooks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("API returns 400 for an invalid book")
    void apiValidatesInput() throws Exception {
        mockMvc.perform(post("/api/books")
                        .with(httpBasic("test", "test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"author\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API returns 404 when deleting a missing book")
    void apiReturnsNotFound() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/books/999")
                        .with(httpBasic("test", "test")))
                .andExpect(status().isNotFound());
    }
}
