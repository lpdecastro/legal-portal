# Session Logout

How `POST /api/v1/auth/logout` ends a client's session. Introduced in AUTH-04-01. Login is described in [`session-login.md`](session-login.md), CSRF handling in [`csrf-protection.md`](csrf-protection.md).

## Request flow

1. The SPA fetches the session's current CSRF token (`GET /api/v1/auth/csrf`), then posts to `/api/v1/auth/logout` with no body, sending the `SESSION` cookie and the `X-XSRF-TOKEN` header. Login replaces the CSRF token, so a token fetched before login doesn't work.
2. The security filter chain rejects the request before it reaches the controller if:
   - the CSRF token is missing or wrong: **403** `FORBIDDEN` ("Invalid or missing CSRF token."). The session is left as it was.
   - the session isn't authenticated (missing, unknown, expired or anonymous): **401** `UNAUTHORIZED` ("Authentication required.").
3. [`LogoutController`](../backend/src/main/java/com/liandrejohn/legalportal/auth/LogoutController.java) calls `SessionLogin.end`, the same method `GET /auth/me` uses for accounts that became inactive. It uses Spring Security's `SecurityContextLogoutHandler`, which invalidates the session and clears the `SecurityContext`.
4. Spring Session's `SessionRepositoryFilter` deletes the invalidated session from the `SPRING_SESSION` table and expires the cookie.
5. The response is **204** with no body and `Set-Cookie: SESSION=; Max-Age=0; Path=/; Secure; HttpOnly; SameSite=Lax`.

If the old cookie value is sent again, it no longer matches a session, so protected APIs return **401**.

Spring Security's built-in `POST /logout` filter stays turned off in [`SecurityConfig`](../backend/src/main/java/com/liandrejohn/legalportal/security/SecurityConfig.java). Logout is a normal API endpoint under `/api/v1`, and its errors use Problem Details like every other endpoint.

## Logging out in the UI

Added in AUTH-04-02.

1. The client picks **Log out** in the header [`ProfileMenu`](../frontend/src/components/ProfileMenu.tsx).
2. [`useLogout`](../frontend/src/hooks/useLogout.ts) calls `logout()` in [`api/auth.ts`](../frontend/src/api/auth.ts). That uses `postNoContent` in [`api/client.ts`](../frontend/src/api/client.ts), which fetches a fresh CSRF token and sends it in `X-XSRF-TOKEN`.
3. While the request runs, the profile button shows a spinner and the menu is disabled, so logout can't be sent twice.
4. On **204**, the user is cleared from auth state and the app navigates to `/login` with `replace`. `RequireAuth` then keeps every protected page out of reach.
5. A **401** means the session had already ended. The `onUnauthorized` listener has signed the client out, so this is treated like a successful logout.
6. Any other failure (403, 5xx, network) shows the toast "We couldn't log you out. Please try again." and leaves the client signed in, because the server session may still be valid.

## Tests

- [`LogoutIntegrationTest`](../backend/src/test/java/com/liandrejohn/legalportal/auth/LogoutIntegrationTest.java) (MockMvc + Testcontainers) covers:
  - a 204 with no body, an expired cookie, and the session row deleted
  - 401 from `GET /auth/me` when the logged-out session is reused
  - 403 Problem Details for a missing, invalid or pre-login CSRF token, with the session kept
  - 401 for an anonymous session
- [`SessionCookieTest`](../backend/src/test/java/com/liandrejohn/legalportal/security/SessionCookieTest.java) checks the cleared cookie's attributes against a real server, because MockMvc doesn't apply `server.servlet.session.cookie`.
- [`ProfileMenu.test.tsx`](../frontend/src/components/ProfileMenu.test.tsx) (RTL + MSW, real route tree) covers the UI: the CSRF header is sent, the client is redirected to `/login`, protected pages stay blocked afterwards, the loading state, error toasts for 403/500/network errors with the client still signed in, and a 401 treated as signed out.
