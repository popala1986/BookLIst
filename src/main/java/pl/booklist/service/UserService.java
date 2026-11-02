package pl.booklist.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.booklist.exception.UsernameTakenException;
import pl.booklist.model.AppUser;
import pl.booklist.model.Role;
import pl.booklist.repository.BookRepository;
import pl.booklist.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Registers users, keeps the single administrator account in place and loads users for Spring Security.
 */
@Service
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public UserService(UserRepository userRepository, BookRepository bookRepository, PasswordEncoder passwordEncoder,
                       @Value("${app.admin.username:}") String adminUsername,
                       @Value("${app.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername.trim();
        this.adminPassword = adminPassword;
    }

    /**
     * Makes sure the administrator account from {@code app.admin.username} exists and is the only admin.
     * The password from {@code app.admin.password} is used only when the account is created;
     * afterwards the admin changes nothing by editing the env file, so a leaked .env cannot reset it.
     * Books added before accounts existed are given to the admin.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureAdminAccount() {
        if (adminUsername.isEmpty()) {
            logger.warn("app.admin.username is not set: no administrator account");
            return;
        }
        AppUser admin = userRepository.findByUsernameIgnoreCase(adminUsername).orElse(null);
        if (admin == null) {
            if (adminPassword.isBlank()) {
                logger.warn("Administrator {} does not exist and app.admin.password is empty: not created", adminUsername);
                return;
            }
            admin = userRepository.save(new AppUser(adminUsername, passwordEncoder.encode(adminPassword), Role.ADMIN));
            logger.info("Created administrator account {}", adminUsername);
        } else if (!admin.isAdmin()) {
            admin.setRole(Role.ADMIN);
            logger.info("Granted administrator role to {}", adminUsername);
        }
        for (AppUser user : userRepository.findAll()) {
            if (user.isAdmin() && !user.getId().equals(admin.getId())) {
                user.setRole(Role.USER);
                logger.info("Removed administrator role from {}", user.getUsername());
            }
        }
        int assigned = bookRepository.assignOwnerlessBooksTo(admin);
        if (assigned > 0) {
            logger.info("Assigned {} books without an owner to {}", assigned, adminUsername);
        }
    }

    /**
     * Creates a regular account. The administrator account cannot be registered through the form.
     */
    @Transactional
    public AppUser register(String username, String rawPassword) {
        String name = username.trim();
        if (userRepository.existsByUsernameIgnoreCase(name) || name.equalsIgnoreCase(adminUsername)) {
            throw new UsernameTakenException(name);
        }
        return userRepository.save(new AppUser(name, passwordEncoder.encode(rawPassword), Role.USER));
    }

    /**
     * Summary of every account for the admin panel.
     */
    @Transactional(readOnly = true)
    public List<UserSummary> findAllUsers() {
        return userRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(u -> new UserSummary(u.getId(), u.getUsername(), u.getRole(),
                        LocalDateTime.ofInstant(u.getCreatedAt(), ZONE),
                        bookRepository.countByOwnerAndOwned(u, true),
                        bookRepository.countByOwnerAndOwned(u, false)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }

    public record UserSummary(Long id, String username, Role role, LocalDateTime createdAt,
                              long ownedBooks, long wishlistBooks) {
    }
}
