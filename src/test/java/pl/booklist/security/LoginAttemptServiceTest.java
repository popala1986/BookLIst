package pl.booklist.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    /** A clock the test can move forward. */
    private static class TestClock extends Clock {
        private Instant now = Instant.parse("2026-11-02T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final TestClock clock = new TestClock();
    private final LoginAttemptService service = new LoginAttemptService(clock, 5, Duration.ofMinutes(15));

    private void fail(String username, String ip, int times) {
        for (int i = 0; i < times; i++) {
            service.loginFailed(username, ip);
        }
    }

    @Test
    @DisplayName("locks after the fifth wrong password, not before")
    void locksAfterMaxAttempts() {
        fail("anna", "1.1.1.1", 4);
        assertThat(service.isBlocked("anna", "1.1.1.1")).isFalse();

        fail("anna", "1.1.1.1", 1);
        assertThat(service.isBlocked("anna", "1.1.1.1")).isTrue();
        assertThat(service.isBlocked("ANNA", "1.1.1.1")).as("username is case-insensitive").isTrue();
    }

    @Test
    @DisplayName("lock applies only to the address that guessed")
    void lockIsPerAddress() {
        fail("anna", "1.1.1.1", 5);

        assertThat(service.isBlocked("anna", "2.2.2.2")).isFalse();
        assertThat(service.isBlocked("jan", "1.1.1.1")).isFalse();
    }

    @Test
    @DisplayName("lock ends after 15 minutes and the counter starts again")
    void lockExpires() {
        fail("anna", "1.1.1.1", 5);
        clock.advance(Duration.ofMinutes(14));
        assertThat(service.isBlocked("anna", "1.1.1.1")).isTrue();

        clock.advance(Duration.ofMinutes(1));
        assertThat(service.isBlocked("anna", "1.1.1.1")).isFalse();

        fail("anna", "1.1.1.1", 1);
        assertThat(service.isBlocked("anna", "1.1.1.1")).isFalse();
    }

    @Test
    @DisplayName("successful login clears earlier failures")
    void successResetsCounter() {
        fail("anna", "1.1.1.1", 4);
        service.loginSucceeded("anna", "1.1.1.1");
        fail("anna", "1.1.1.1", 4);

        assertThat(service.isBlocked("anna", "1.1.1.1")).isFalse();
    }

    @Test
    @DisplayName("old failures are forgotten after a quiet period")
    void oldFailuresAreForgotten() {
        fail("anna", "1.1.1.1", 4);
        clock.advance(Duration.ofMinutes(16));
        fail("anna", "1.1.1.1", 1);

        assertThat(service.isBlocked("anna", "1.1.1.1")).isFalse();
    }

    @Test
    @DisplayName("unblock lifts the lock from every address")
    void unblock() {
        fail("anna", "1.1.1.1", 5);
        fail("anna", "2.2.2.2", 5);
        fail("annabel", "1.1.1.1", 5);

        service.unblock("Anna");

        assertThat(service.isBlocked("anna", "1.1.1.1")).isFalse();
        assertThat(service.isBlocked("anna", "2.2.2.2")).isFalse();
        assertThat(service.isBlocked("annabel", "1.1.1.1")).isTrue();
    }
}
