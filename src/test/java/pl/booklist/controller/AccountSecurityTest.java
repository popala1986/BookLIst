package pl.booklist.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pl.booklist.model.AppUser;
import pl.booklist.model.Role;
import pl.booklist.repository.UserRepository;
import pl.booklist.service.CoverService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Adding users by the administrator, changing passwords and locking the login after wrong passwords.
 */
@SpringBootTest(properties = "app.registration.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AccountSecurityTest {

    private static final String BOOKS = "/api/books/showAllBooks";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private CoverService coverService;

    private void addUser(String username, String password) throws Exception {
        mockMvc.perform(post("/admin/users").with(httpBasic("admin", "admin-password")).with(csrf())
                        .param("username", username)
                        .param("password", password)
                        .param("confirmPassword", password))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("message", containsString(username)));
    }

    private Long idOf(String username) {
        return userRepository.findByUsernameIgnoreCase(username).map(AppUser::getId).orElseThrow();
    }

    @Test
    @DisplayName("registration is off: the form creates no account and the login page has no link to it")
    void registrationIsOff() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "obcy")
                        .param("password", "bardzo-tajne")
                        .param("confirmPassword", "bardzo-tajne"))
                .andExpect(status().isOk());
        assertThat(userRepository.findByUsernameIgnoreCase("obcy")).isEmpty();

        mockMvc.perform(get("/login"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Załóż konto"))));
    }

    @Test
    @DisplayName("administrator adds a user who can then log in")
    void adminAddsUser() throws Exception {
        addUser("anna", "haslo-startowe");

        assertThat(userRepository.findByUsernameIgnoreCase("anna")).get()
                .extracting(AppUser::getRole).isEqualTo(Role.USER);
        mockMvc.perform(get(BOOKS).with(httpBasic("anna", "haslo-startowe"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("adding a user validates the form and rejects taken names")
    void addUserValidates() throws Exception {
        addUser("anna", "haslo-startowe");

        mockMvc.perform(post("/admin/users").with(httpBasic("admin", "admin-password")).with(csrf())
                        .param("username", "ANNA")
                        .param("password", "haslo-startowe")
                        .param("confirmPassword", "haslo-startowe"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attributeHasFieldErrors("form", "username"));

        mockMvc.perform(post("/admin/users").with(httpBasic("admin", "admin-password")).with(csrf())
                        .param("username", "jan")
                        .param("password", "krotkie")
                        .param("confirmPassword", "inne"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "password", "confirmPassword"));
        assertThat(userRepository.findByUsernameIgnoreCase("jan")).isEmpty();
    }

    @Test
    @DisplayName("a regular user cannot add users or set passwords")
    void onlyAdminManagesUsers() throws Exception {
        addUser("anna", "haslo-startowe");

        mockMvc.perform(post("/admin/users").with(httpBasic("anna", "haslo-startowe")).with(csrf())
                        .param("username", "jan")
                        .param("password", "haslo-startowe")
                        .param("confirmPassword", "haslo-startowe"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/users/" + idOf("admin") + "/password")
                        .with(httpBasic("anna", "haslo-startowe")).with(csrf())
                        .param("password", "przejete-konto"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("user changes their own password after giving the current one")
    void changePassword() throws Exception {
        addUser("anna", "haslo-startowe");

        mockMvc.perform(get("/account/password").with(httpBasic("anna", "haslo-startowe")))
                .andExpect(status().isOk())
                .andExpect(view().name("account-password"));

        mockMvc.perform(post("/account/password").with(httpBasic("anna", "haslo-startowe")).with(csrf())
                        .param("currentPassword", "zle-haslo")
                        .param("newPassword", "nowe-haslo-anny")
                        .param("confirmPassword", "nowe-haslo-anny"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "currentPassword"));

        mockMvc.perform(post("/account/password").with(httpBasic("anna", "haslo-startowe")).with(csrf())
                        .param("currentPassword", "haslo-startowe")
                        .param("newPassword", "nowe-haslo-anny")
                        .param("confirmPassword", "nowe-haslo-anny"))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("message"));

        mockMvc.perform(get(BOOKS).with(httpBasic("anna", "haslo-startowe"))).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BOOKS).with(httpBasic("anna", "nowe-haslo-anny"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("five wrong passwords lock the login, even with the right password")
    void lockAfterFiveWrongPasswords() throws Exception {
        addUser("anna", "haslo-startowe");

        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(formLogin().user("anna").password("zle-haslo-" + i))
                    .andExpect(redirectedUrl("/login?error"));
        }
        mockMvc.perform(formLogin().user("anna").password("zle-haslo-5"))
                .andExpect(redirectedUrl("/login?locked"));
        mockMvc.perform(formLogin().user("anna").password("haslo-startowe"))
                .andExpect(redirectedUrl("/login?locked"));
        mockMvc.perform(get(BOOKS).with(httpBasic("anna", "haslo-startowe")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/login").param("locked", ""))
                .andExpect(content().string(containsString("Spróbuj ponownie za 15 minut")));
    }

    @Test
    @DisplayName("a successful login resets the counter")
    void successResetsCounter() throws Exception {
        addUser("anna", "haslo-startowe");

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(formLogin().user("anna").password("zle")).andExpect(redirectedUrl("/login?error"));
        }
        mockMvc.perform(formLogin().user("anna").password("haslo-startowe")).andExpect(redirectedUrl("/"));
        mockMvc.perform(formLogin().user("anna").password("zle")).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("administrator sets a new password, which also lifts the lock")
    void adminSetsPassword() throws Exception {
        addUser("anna", "haslo-startowe");
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(formLogin().user("anna").password("zle"));
        }

        mockMvc.perform(post("/admin/users/" + idOf("anna") + "/password")
                        .with(httpBasic("admin", "admin-password")).with(csrf())
                        .param("password", "krotkie"))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("error"));

        mockMvc.perform(post("/admin/users/" + idOf("anna") + "/password")
                        .with(httpBasic("admin", "admin-password")).with(csrf())
                        .param("password", "nowe-haslo-od-admina"))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("message", containsString("anna")));

        mockMvc.perform(formLogin().user("anna").password("nowe-haslo-od-admina")).andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("admin panel shows the add-user form and a password field for every user")
    void adminPanelRenders() throws Exception {
        addUser("anna", "haslo-startowe");

        mockMvc.perform(get("/admin").with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dodaj użytkownika")))
                .andExpect(content().string(containsString("/admin/users/" + idOf("anna") + "/password")));
    }
}
