# Session Login

How `POST /api/v1/auth/login` authenticates a client and starts a session. Introduced in AUTH-02-01. CSRF handling is described in [`csrf-protection.md`](csrf-protection.md).

## Request flow

1. The SPA fetches a CSRF token (`GET /api/v1/auth/csrf`), then posts `{ "email", "password" }` with the `SESSION` cookie and the `X-XSRF-TOKEN` header. `CsrfFilter` rejects the request with 403 if the token is missing or wrong.
2. [`LoginController`](../backend/src/main/java/com/liandrejohn/legalportal/auth/LoginController.java) validates the body. Both fields are required, and `LoginRequest` trims and lowercases the email the same way registration does.
3. [`LoginService`](../backend/src/main/java/com/liandrejohn/legalportal/auth/LoginService.java):
   - asks `LoginAttemptLimiter` whether the email or client IP is blocked. If so, it returns **429** `RATE_LIMIT_EXCEEDED` without checking the password.
   - calls the `AuthenticationManager` (a `DaoAuthenticationProvider` with BCrypt). [`ClientUserDetailsService`](../backend/src/main/java/com/liandrejohn/legalportal/security/ClientUserDetailsService.java) loads the user by email and skips soft-deleted users. `SUSPENDED` users are marked as locked and `DISABLED` users as disabled.
   - turns every `AuthenticationException` into **401** `UNAUTHORIZED` with "Invalid email or password.", and records a failure. Unknown emails, wrong passwords and inactive accounts produce the same response.
   - after a success, clears the email's failures and loads the client profile for the response body.
4. [`SessionLogin`](../backend/src/main/java/com/liandrejohn/legalportal/security/SessionLogin.java) stores the authentication:
   - `ChangeSessionIdAuthenticationStrategy` gives the session a new ID, which guards against session fixation. The old `SESSION` cookie value stops working.
   - `CsrfAuthenticationStrategy` removes the old CSRF token. A new one is created on the next `GET /api/v1/auth/csrf`.
   - The `SecurityContext` is saved through the same `SecurityContextRepository` the filter chain reads from. Spring Session JDBC persists it in PostgreSQL.
5. The response is **200** with `{ id, email, firstName, lastName }` and a new `SESSION` cookie (`HttpOnly; Secure; SameSite=Lax; Path=/`). Later requests that carry the cookie are authenticated.

## Inactive accounts and timing

Spring's provider normally checks account status before the password. Here, status is checked only after the password matches. As a result, an inactive account costs the same BCrypt work as a wrong password, and its status can't be probed without knowing the password. For unknown emails, `DaoAuthenticationProvider` already runs a dummy BCrypt check.

## Rate limiting

[`LoginAttemptLimiter`](../backend/src/main/java/com/liandrejohn/legalportal/auth/LoginAttemptLimiter.java) counts failed logins in memory, in a fixed window that starts at the first failure. It counts per email and per client IP:

| Setting (`app.security.login-rate-limit.*`) | Default | Meaning |
| --- | --- | --- |
| `max-failures-per-email` | 5 | Failures for one email before it is blocked, from any IP |
| `max-failures-per-ip` | 50 | Failures from one IP, across all emails, before it is blocked |
| `window` | 15m | How long failures are counted |

- Emails are counted whether or not an account exists, so a 429 doesn't reveal which emails are registered.
- A successful login clears that email's count. The IP count keeps running.
- Counts live in each backend instance. With several ECS tasks, each one enforces the limits separately.
- The client IP comes from `request.getRemoteAddr()`. `server.forward-headers-strategy: native` lets Tomcat take it from `X-Forwarded-For` when the request comes from an internal proxy address, such as the ALB.

## Tests

- [`LoginIntegrationTest`](../backend/src/test/java/com/liandrejohn/legalportal/auth/LoginIntegrationTest.java) (MockMvc + Testcontainers): success and session recognition, session ID change, CSRF token replacement, email normalization, generic 401s (wrong password, unknown email, suspended, disabled, deleted, password over 72 bytes), rate limiting for known and unknown emails, validation, and a missing CSRF token. Each test uses its own client IP so the IP counts don't carry over between tests.
- [`LoginServiceTest`](../backend/src/test/java/com/liandrejohn/legalportal/auth/LoginServiceTest.java) and [`LoginAttemptLimiterTest`](../backend/src/test/java/com/liandrejohn/legalportal/auth/LoginAttemptLimiterTest.java): unit tests. The limiter test uses a controllable clock.
- [`SessionCookieTest`](../backend/src/test/java/com/liandrejohn/legalportal/security/SessionCookieTest.java) checks against a real server that login sets a new `SESSION` cookie with the right flags.
