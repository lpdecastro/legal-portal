package com.liandrejohn.legalportal.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Limits on failed login attempts within a fixed window, counted per email and per client IP.
 *
 * @param maxFailuresPerEmail failures allowed for one email before further attempts are rejected
 * @param maxFailuresPerIp failures allowed from one IP before further attempts are rejected
 * @param window how long failures are counted, starting at the first failure
 */
@ConfigurationProperties("app.security.login-rate-limit")
public record LoginRateLimitProperties(
        @DefaultValue("5") int maxFailuresPerEmail,
        @DefaultValue("50") int maxFailuresPerIp,
        @DefaultValue("15m") Duration window) {
}
