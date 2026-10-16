# CSRF Protection

How the backend protects state-changing requests from cross-site request forgery (CSRF). Introduced in AUTH-00-01.

CSRF tokens are stored in the server-side session. Spring Session JDBC persists that session in PostgreSQL. The SPA fetches a token from `GET /api/v1/auth/csrf` and sends it back in the `X-XSRF-TOKEN` header. Security rules are configured in [`SecurityConfig`](../backend/src/main/java/com/liandrejohn/legalportal/security/SecurityConfig.java).

## 1. Getting a token: `GET /api/v1/auth/csrf`

1. `SessionRepositoryFilter` (Spring Session) wraps the request. It asks `CookieHttpSessionIdResolver` for a `SESSION` cookie. The first time, there isn't one.
2. Spring Security's filter chain runs:
   - `CsrfFilter` asks `HttpSessionCsrfTokenRepository` for a token. The repository returns a deferred (lazy) token. `XorCsrfTokenRequestAttributeHandler` puts it on the request as a `CsrfToken` attribute. GET requests are not checked.
   - `AuthorizationFilter` lets the request through, because this endpoint is `permitAll()`.
3. Spring MVC calls `CsrfController.csrf(CsrfToken)`. `CsrfTokenArgumentResolver` supplies the `CsrfToken` argument from the request attribute.
4. Calling `csrfToken.getToken()` makes the token real:
   - `HttpSessionCsrfTokenRepository` generates a random token and calls `saveToken()`, which stores it in the HTTP session. Storing it creates the session, through the request wrapper from `SessionRepositoryFilter`.
   - `XorCsrfTokenRequestAttributeHandler` masks the value it returns, to protect against the BREACH attack. The string is different on every call but always unmasks to the same token stored in the session.
5. The controller returns a `CsrfTokenResponse` record, serialized as JSON:

   ```json
   { "token": "<masked-token>", "headerName": "X-XSRF-TOKEN" }
   ```

6. When the request finishes, `SessionRepositoryFilter` commits the session:
   - `JdbcIndexedSessionRepository` writes it to the `spring_session` table.
   - `DefaultCookieSerializer` sets `SESSION=<id>; Path=/; Secure; HttpOnly; SameSite=Lax`. The cookie flags come from `server.servlet.session.cookie.*` in `application.yaml`.

## 2. Sending a state-changing request (POST/PUT/PATCH/DELETE)

1. The browser sends the `SESSION` cookie automatically. The SPA sets the `X-XSRF-TOKEN` header to the token it fetched.
2. `SessionRepositoryFilter` reads the cookie, and `JdbcIndexedSessionRepository` loads the session from PostgreSQL.
3. `CsrfFilter` checks the token:
   - It loads the expected token from the session through `HttpSessionCsrfTokenRepository`.
   - `XorCsrfTokenRequestAttributeHandler` reads the header and unmasks it.
   - The two values are compared in constant time.
4. **Check fails:** `CsrfFilter` stops the request and passes one of two exceptions to `ProblemDetailsAccessDeniedHandler`:
   - `MissingCsrfTokenException`: the session has no token, for example because the request had no session cookie.
   - `InvalidCsrfTokenException`: the header is missing, the value is wrong, or the token belongs to a different session.

   Both exceptions extend `CsrfException`. The handler returns **403** with `errorCode: FORBIDDEN` and `detail: "Invalid or missing CSRF token."`.
5. **Check passes:** the request continues to `AuthorizationFilter`. Every endpoint that isn't public requires authentication:
   - **Authenticated:** the request reaches the controller, where validation and business logic run as usual.
   - **Not authenticated:** `AuthorizationFilter` throws `AccessDeniedException`. `ExceptionTranslationFilter` sees an anonymous user and calls `ProblemDetailsAuthenticationEntryPoint`, which returns **401** with `errorCode: UNAUTHORIZED`. The separate 401 keeps "not logged in" distinguishable from a CSRF failure.

## 3. Error responses

1. `ProblemDetailsAccessDeniedHandler` and `ProblemDetailsAuthenticationEntryPoint` both call `ProblemDetailsResponseWriter.write(...)`. These handlers run inside the security filters, before Spring MVC's exception handling.
2. The writer calls `ProblemDetails.of(...)`, which builds a Spring `ProblemDetail` and adds:
   - `instance`: the request path
   - `errorCode`
   - `timestamp`
   - `traceId`: taken from the logging context (MDC) if present, otherwise generated
3. The writer logs a warning that includes the `traceId`. It then serializes the body with Jackson's `JsonMapper` as `application/problem+json`.

Example 403 body:

```json
{
  "detail": "Invalid or missing CSRF token.",
  "instance": "/api/v1/cases",
  "status": 403,
  "title": "Forbidden",
  "errorCode": "FORBIDDEN",
  "timestamp": "2026-10-16T00:21:05.436987Z",
  "traceId": "bd031f5face64925"
}
```

Spring omits `type` when it is `about:blank`. RFC 9457 treats a missing `type` as `about:blank`, so the two are equivalent.

## Why this stops CSRF

A malicious site can make the browser send the `SESSION` cookie. It cannot read the response from `/api/v1/auth/csrf`, because the browser's same-origin policy blocks that. Without the token it cannot set `X-XSRF-TOKEN`, so `CsrfFilter` rejects its requests with 403. `SameSite=Lax` on the cookie adds a second layer: browsers don't send the cookie on cross-site POSTs at all.

## Configuration

- **Session storage:** the `SPRING_SESSION` tables are created by the Flyway migration `V1__create_spring_session_tables.sql`; Spring Session's own initializer is off (`spring.session.jdbc.initialize-schema: never`).
- **Cookie flags:** `server.servlet.session.cookie.http-only`, `secure` and `same-site` set the flags on Spring Session's cookie.
- **Public endpoints:** `GET /api/v1/auth/csrf`, `POST /api/v1/auth/register`, `POST /api/v1/auth/login` and `GET /actuator/health/**`. Every other request requires authentication. The public POSTs still need a CSRF token.
- **Token rotation on login:** a successful login replaces the CSRF token (and the session ID). The SPA must fetch a new token from `GET /api/v1/auth/csrf` before its next state-changing request. See [`session-login.md`](session-login.md).
- **Disabled defaults:** Spring Security's default `POST /logout` filter is turned off, because logout will get its own API endpoint. The request cache is also turned off.

## Tests

Docker must be running. Both test classes use Testcontainers PostgreSQL, so sessions go through the real JDBC repository.

- [`CsrfProtectionTest`](../backend/src/test/java/com/liandrejohn/legalportal/security/CsrfProtectionTest.java) uses MockMvc and a test-only `POST /api/v1/test/csrf-probe` endpoint. It fetches a real `SESSION` cookie and token, then covers:
  - getting a token without logging in
  - a valid token with an authenticated user (204)
  - a valid token without authentication (401)
  - a missing token, an invalid token, a token from another session, and a token sent without a session (all 403)
- [`SessionCookieTest`](../backend/src/test/java/com/liandrejohn/legalportal/security/SessionCookieTest.java) checks the `Set-Cookie` flags against a real server (`RANDOM_PORT`). MockMvc's mock environment doesn't apply the `server.servlet.session.cookie.*` flags, so this test can't use MockMvc.
