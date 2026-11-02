package pl.booklist.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import pl.booklist.security.LoginAttemptService;
import pl.booklist.service.UserService;

import java.util.UUID;

/**
 * Security configuration.
 * <ul>
 *     <li>Web UI: custom login page at {@code /login}, "remember me" for 30 days. New accounts are added
 *     by the administrator; public registration at {@code /register} is off unless
 *     {@code app.registration.enabled=true}.</li>
 *     <li>Too many wrong passwords lock the login for a while, see {@link LoginAttemptService}.</li>
 *     <li>{@code /admin/**}: only the single administrator account ({@code app.admin.username}).</li>
 *     <li>REST API under {@code /api/**}: HTTP Basic with the same accounts, no CSRF.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private static final int REMEMBER_ME_SECONDS = 30 * 24 * 60 * 60;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   UserService userService,
                                                   LoginAttemptService loginAttempts,
                                                   @Value("${app.remember-me-key:}") String rememberMeKey) throws Exception {
        String key = rememberMeKey.isBlank() ? UUID.randomUUID().toString() : rememberMeKey;
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.svg",
                                "/login", "/register", "/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)
                        .failureHandler(loginFailureHandler(loginAttempts)))
                .rememberMe(remember -> remember
                        .key(key)
                        .userDetailsService(userService)
                        .tokenValiditySeconds(REMEMBER_ME_SECONDS))
                .httpBasic(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
        return http.build();
    }

    /**
     * Back to the login page with {@code ?locked} when the login is locked (also right after the last
     * allowed attempt), otherwise with {@code ?error}.
     */
    private static AuthenticationFailureHandler loginFailureHandler(LoginAttemptService loginAttempts) {
        return (request, response, exception) -> {
            String username = String.valueOf(request.getParameter("username"));
            boolean locked = exception instanceof LockedException
                    || loginAttempts.isBlocked(username, request.getRemoteAddr());
            response.sendRedirect(request.getContextPath() + (locked ? "/login?locked" : "/login?error"));
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
