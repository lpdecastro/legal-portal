# Session Restoration

How the SPA keeps a client signed in across page refreshes. Introduced in AUTH-03-01. Login itself is described in [`session-login.md`](session-login.md).

The session lives on the server (Spring Session JDBC) and is identified by the HttpOnly `SESSION` cookie, which JavaScript can't read. After a refresh, React state is empty, so the SPA asks the server who the cookie belongs to.

## Backend: `GET /api/v1/auth/me`

- No request body. The browser sends the existing `SESSION` cookie.
- Requests without an authenticated session never reach the controller. The security filter chain answers with **401** `UNAUTHORIZED` ("Authentication required.") from [`ProblemDetailsAuthenticationEntryPoint`](../backend/src/main/java/com/liandrejohn/legalportal/security/ProblemDetailsAuthenticationEntryPoint.java). This covers a missing cookie, an unknown session ID, an expired session (Spring Session deletes it on lookup), and an anonymous session such as one created by `GET /auth/csrf`.
- [`CurrentClientController`](../backend/src/main/java/com/liandrejohn/legalportal/auth/CurrentClientController.java) takes the user ID from the session's `AuthenticatedUser`. [`CurrentClientService`](../backend/src/main/java/com/liandrejohn/legalportal/auth/CurrentClientService.java) then reloads the user and client profile from the database, so name changes show up right away.
- If the account was deleted, suspended or disabled after login, the session is invalidated (`SessionLogin.end`) and the response is the same 401. A later reactivation doesn't bring that session back.
- A success returns **200** with `{ id, email, firstName, lastName }` ([`AuthenticatedClientResponse`](../backend/src/main/java/com/liandrejohn/legalportal/auth/AuthenticatedClientResponse.java)), the same shape login returns.
- It's a `GET`, so no CSRF token is needed.

## Frontend

- [`AuthProvider`](../frontend/src/auth/AuthProvider.tsx) calls `fetchCurrentUser()` ([`api/auth.ts`](../frontend/src/api/auth.ts)) once on start-up and shows a full-screen `Spin` ("Loading…") until it settles. Nothing else renders while the check runs, so pages never see a half-restored state.
- A 200 response puts the client into auth state. A 401, any other error or a network failure leaves the client signed out.
- In development, `StrictMode` runs the effect twice, so `/auth/me` is requested twice. Only the second result is applied.
- The profile menu shows the signed-in client's name.

## Tests

- [`CurrentClientIntegrationTest`](../backend/src/test/java/com/liandrejohn/legalportal/auth/CurrentClientIntegrationTest.java) (MockMvc + Testcontainers) covers:
  - a valid session, and profile changes showing up on the next call
  - 401 Problem Details for a missing, unknown, anonymous or expired session
  - suspended, disabled and deleted accounts, including the session staying ended after reactivation
- [`AuthProvider.test.tsx`](../frontend/src/auth/AuthProvider.test.tsx) (RTL + MSW) covers the loading state then the restored user, and staying signed out on 401, 500 or a network error.
- `renderWithProviders` takes a `user` option (default `null`) and skips the `/auth/me` call. Component tests can then start signed in or signed out without mocking the session check.
