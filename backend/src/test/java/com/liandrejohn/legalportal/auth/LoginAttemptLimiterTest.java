package com.liandrejohn.legalportal.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {

    private static final String EMAIL = "john@example.com";

    private static final String IP = "203.0.113.10";

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-16T10:00:00Z"));

    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(
            new LoginRateLimitProperties(3, 5, Duration.ofMinutes(15)), clock);

    @Test
    void blocksEmailAfterMaxFailures() {
        failTimes(EMAIL, IP, 2);
        assertThat(limiter.isBlocked(EMAIL, IP)).isFalse();

        limiter.recordFailure(EMAIL, IP);
        assertThat(limiter.isBlocked(EMAIL, IP)).isTrue();
        // The email is blocked from any IP
        assertThat(limiter.isBlocked(EMAIL, "198.51.100.7")).isTrue();
        assertThat(limiter.isBlocked("jane@example.com", "198.51.100.7")).isFalse();
    }

    @Test
    void blocksIpAfterMaxFailuresAcrossEmails() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("user" + i + "@example.com", IP);
        }

        assertThat(limiter.isBlocked("someone-else@example.com", IP)).isTrue();
        assertThat(limiter.isBlocked("someone-else@example.com", "198.51.100.7")).isFalse();
    }

    @Test
    void failuresExpireAfterWindow() {
        failTimes(EMAIL, IP, 3);

        clock.advance(Duration.ofMinutes(15).minusSeconds(1));
        assertThat(limiter.isBlocked(EMAIL, IP)).isTrue();

        clock.advance(Duration.ofSeconds(1));
        assertThat(limiter.isBlocked(EMAIL, IP)).isFalse();

        // A failure after expiry starts a new window
        limiter.recordFailure(EMAIL, IP);
        assertThat(limiter.isBlocked(EMAIL, IP)).isFalse();
    }

    @Test
    void successClearsEmailFailuresButNotIpFailures() {
        failTimes(EMAIL, IP, 2);
        limiter.recordSuccess(EMAIL);

        failTimes(EMAIL, IP, 2);
        assertThat(limiter.isBlocked(EMAIL, IP)).isFalse();

        // 4 IP failures so far; one more reaches the IP limit
        limiter.recordFailure("jane@example.com", IP);
        assertThat(limiter.isBlocked("other@example.com", IP)).isTrue();
    }

    private void failTimes(String email, String ip, int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(email, ip);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

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

}
