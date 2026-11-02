package pl.booklist.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protects passwords against guessing: after {@code app.login.max-attempts} wrong passwords
 * for the same username from the same IP address, that username is locked on that address
 * for {@code app.login.lock-minutes}. A successful login clears the counter.
 * <p>
 * Counters are kept in memory, so a restart clears them. Locking per IP address means that
 * someone guessing passwords from their own computer cannot lock the real owner out.
 */
@Service
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);
    /** Upper bound for remembered counters, so a flood of random usernames cannot fill the memory. */
    private static final int MAX_ENTRIES = 10_000;

    private final Clock clock;
    private final int maxAttempts;
    private final Duration lockDuration;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptService(@Value("${app.login.max-attempts:5}") int maxAttempts,
                               @Value("${app.login.lock-minutes:15}") int lockMinutes) {
        this(Clock.systemUTC(), maxAttempts, Duration.ofMinutes(lockMinutes));
    }

    LoginAttemptService(Clock clock, int maxAttempts, Duration lockDuration) {
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
    }

    public void loginFailed(String username, String ip) {
        Instant now = clock.instant();
        if (attempts.size() >= MAX_ENTRIES) {
            attempts.values().removeIf(a -> a.isExpired(now, lockDuration));
        }
        Attempts updated = attempts.compute(key(username, ip), (k, current) -> {
            if (current == null || current.isExpired(now, lockDuration)) {
                return new Attempts(1, null, now);
            }
            int failures = current.failures() + 1;
            Instant lockedUntil = failures >= maxAttempts ? now.plus(lockDuration) : null;
            return new Attempts(failures, lockedUntil, now);
        });
        if (updated.lockedUntil() != null && updated.failures() == maxAttempts) {
            logger.warn("Zablokowano logowanie na {} min: użytkownik {} z adresu {} ({} błędnych haseł)",
                    lockDuration.toMinutes(), username, ip, maxAttempts);
        }
    }

    public void loginSucceeded(String username, String ip) {
        attempts.remove(key(username, ip));
    }

    public boolean isBlocked(String username, String ip) {
        Attempts current = attempts.get(key(username, ip));
        return current != null && current.lockedUntil() != null && current.lockedUntil().isAfter(clock.instant());
    }

    /** Lets the user log in again from every address, e.g. after the admin sets a new password. */
    public void unblock(String username) {
        String prefix = username.toLowerCase(Locale.ROOT) + "|";
        attempts.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /** IP address of the current HTTP request (the real client, see server.forward-headers-strategy). */
    public static String currentClientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRemoteAddr();
        }
        return "unknown";
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        Authentication auth = event.getAuthentication();
        loginFailed(auth.getName(), ipOf(auth));
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        loginSucceeded(auth.getName(), ipOf(auth));
    }

    private static String ipOf(Authentication auth) {
        if (auth.getDetails() instanceof WebAuthenticationDetails details && details.getRemoteAddress() != null) {
            return details.getRemoteAddress();
        }
        return currentClientIp();
    }

    private static String key(String username, String ip) {
        return String.valueOf(username).trim().toLowerCase(Locale.ROOT) + "|" + ip;
    }

    private record Attempts(int failures, Instant lockedUntil, Instant lastFailure) {
        /** A lock ends after its time; failures without a lock are forgotten after the same quiet period. */
        boolean isExpired(Instant now, Duration window) {
            Instant until = lockedUntil != null ? lockedUntil : lastFailure.plus(window);
            return !until.isAfter(now);
        }
    }
}
