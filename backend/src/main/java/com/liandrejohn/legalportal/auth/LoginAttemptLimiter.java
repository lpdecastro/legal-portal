package com.liandrejohn.legalportal.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Counts failed logins per email and per client IP in memory and blocks further attempts once a limit is reached.
 * Emails are counted whether or not they belong to an account, so the limit doesn't reveal which ones exist.
 * <p>
 * State is per instance: with several backend instances each one enforces the limits separately.
 */
@Component
@EnableConfigurationProperties(LoginRateLimitProperties.class)
class LoginAttemptLimiter {

    /** Above this many tracked keys, expired windows are purged on the next failure. */
    private static final int PURGE_THRESHOLD = 10_000;

    private final Map<String, FailureWindow> failures = new ConcurrentHashMap<>();

    private final LoginRateLimitProperties properties;

    private final Clock clock;

    @Autowired
    LoginAttemptLimiter(LoginRateLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    LoginAttemptLimiter(LoginRateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    boolean isBlocked(String email, String clientIp) {
        Instant now = clock.instant();
        return failureCount(emailKey(email), now) >= properties.maxFailuresPerEmail()
                || failureCount(ipKey(clientIp), now) >= properties.maxFailuresPerIp();
    }

    void recordFailure(String email, String clientIp) {
        Instant now = clock.instant();
        if (failures.size() > PURGE_THRESHOLD) {
            failures.values().removeIf((window) -> window.isExpired(now, properties));
        }
        increment(emailKey(email), now);
        increment(ipKey(clientIp), now);
    }

    /** Clears the email's failures after a successful login. The IP's failures still count. */
    void recordSuccess(String email) {
        failures.remove(emailKey(email));
    }

    private int failureCount(String key, Instant now) {
        FailureWindow window = failures.get(key);
        return window == null || window.isExpired(now, properties) ? 0 : window.count();
    }

    private void increment(String key, Instant now) {
        failures.compute(key, (k, window) -> window == null || window.isExpired(now, properties)
                ? new FailureWindow(now, 1) : new FailureWindow(window.start(), window.count() + 1));
    }

    private static String emailKey(String email) {
        return "email:" + email;
    }

    private static String ipKey(String clientIp) {
        return "ip:" + clientIp;
    }

    private record FailureWindow(Instant start, int count) {

        boolean isExpired(Instant now, LoginRateLimitProperties properties) {
            return !now.isBefore(start.plus(properties.window()));
        }

    }

}
