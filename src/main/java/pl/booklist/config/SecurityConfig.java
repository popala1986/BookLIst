package pl.booklist.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import pl.booklist.service.UserService;

import java.util.UUID;

/**
 * Security configuration.
 * <ul>
 *     <li>Web UI: custom login page at {@code /login}, registration at {@code /register}
 *     (can be switched off with {@code app.registration.enabled=false}), "remember me" for 30 days.</li>
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
                        .defaultSuccessUrl("/", false))
                .rememberMe(remember -> remember
                        .key(key)
                        .userDetailsService(userService)
                        .tokenValiditySeconds(REMEMBER_ME_SECONDS))
                .httpBasic(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
