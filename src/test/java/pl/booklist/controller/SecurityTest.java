package pl.booklist.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pl.booklist.model.AppUser;
import pl.booklist.model.Role;
import pl.booklist.repository.BookRepository;
import pl.booklist.repository.UserRepository;
import pl.booklist.service.CoverService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @MockBean
    private CoverService coverService;

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", username)
                        .param("password", password)
                        .param("confirmPassword", password))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("anonymous user is redirected to the login page")
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("login and registration pages are public")
    void loginAndRegisterArePublic() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("administrator account is created at startup")
    void adminAccountIsCreated() {
        AppUser admin = userRepository.findByUsernameIgnoreCase("admin").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("registration creates a regular user who can log in")
    void registrationCreatesUser() throws Exception {
        register("anna", "bardzo-tajne");

        assertThat(userRepository.findByUsernameIgnoreCase("anna")).get()
                .extracting(AppUser::getRole).isEqualTo(Role.USER);
        mockMvc.perform(get("/api/books/showAllBooks").with(httpBasic("anna", "bardzo-tajne")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("registration rejects mismatched passwords and the admin username")
    void registrationValidates() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "anna")
                        .param("password", "bardzo-tajne")
                        .param("confirmPassword", "inne-haslo"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "confirmPassword"));

        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "ADMIN")
                        .param("password", "bardzo-tajne")
                        .param("confirmPassword", "bardzo-tajne"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "username"));
    }

    @Test
    @DisplayName("only the administrator can open the admin panel")
    void adminPanelIsRestricted() throws Exception {
        mockMvc.perform(get("/admin").with(user("anna").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin").with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"));
    }

    @Test
    @DisplayName("users only see and change their own books")
    void usersAreIsolated() throws Exception {
        when(coverService.fetchCoverUrl(anyString(), anyString())).thenReturn(CoverService.DEFAULT_COVER_URL);
        register("anna", "bardzo-tajne");
        register("jan", "bardzo-tajne");

        mockMvc.perform(post("/api/books").with(httpBasic("anna", "bardzo-tajne"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Lód\",\"author\":\"Dukaj\"}"))
                .andExpect(status().isCreated());
        Long annasBookId = bookRepository.findByOwnerUsernameOrderByTitleAsc("anna").get(0).getId();

        mockMvc.perform(get("/api/books/showAllBooks").with(httpBasic("jan", "bardzo-tajne")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mockMvc.perform(delete("/api/books/" + annasBookId).with(httpBasic("jan", "bardzo-tajne")))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/books/" + annasBookId).with(httpBasic("anna", "bardzo-tajne")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("API returns 400 for an invalid book")
    void apiValidatesInput() throws Exception {
        mockMvc.perform(post("/api/books")
                        .with(httpBasic("admin", "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"author\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("pages render for a logged-in user")
    void pagesRender() throws Exception {
        mockMvc.perform(get("/").with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Moja biblioteka")));
        mockMvc.perform(get("/add").with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("adding a book through the form validates input")
    void addFormValidates() throws Exception {
        mockMvc.perform(post("/add").with(httpBasic("admin", "admin-password")).with(csrf())
                        .param("title", "")
                        .param("author", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("book-add"))
                .andExpect(model().attributeHasFieldErrors("newBook", "title", "author"));
    }
}
