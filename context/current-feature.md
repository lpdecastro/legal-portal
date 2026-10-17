# Current Feature: AUTH-03-02 Protected Routes

Prevent unauthenticated clients from accessing protected pages.

## Status

In Progress

## Goals

- Protected routes implemented with React Router, using the auth state from AUTH-03-01 (`useAuth` / `AuthProvider`).
- Authenticated clients can access protected pages.
- Unauthenticated clients are redirected to `/login`.
- Refreshing a protected page does not redirect clients with valid sessions.
- Expired or invalid sessions redirect clients to `/login`.
- Protected pages are not displayed while authentication is being checked (loading state shown instead).
- Automated tests added and passing.

## Notes

- Type: Frontend only. Spec: `context/epics/02-authentication/stories/AUTH-03-session/tasks/AUTH-03-02-protected-routes.md`.
- Out of scope: backend authentication/authorization, session creation and restoration API, login and logout functionality.
- AUTH-03-01 already restores the client on start-up via `fetchCurrentUser()` behind a full-screen `Spin`; any failure leaves them signed out. The guard should build on that state rather than re-fetching.
- "Expired or invalid sessions" covers the start-up check failing (401 from `GET /auth/me`) — those clients must end up on `/login`.
- `renderWithProviders` takes a `user` option that skips the session check — use it for authenticated route tests; use MSW for the unauthenticated/expired cases.
- Routes are centralized in `routes/paths.ts`; protected pages sit under `AuthenticatedLayout`, public auth pages under `PublicLayout`.

## History

<!-- Keep this updated. Earliest to latest -->

- **FND-03: PostgreSQL Setup** (2026-10-15) — Docker Compose PostgreSQL 17 service, Spring Data JPA + PostgreSQL driver, datasource configured from env vars via git-ignored repo-root `.env` (template in `.env.example`).
- **FND-04: React Routing** (2026-10-15) — React Router (`react-router` 8) browser router with placeholder pages for every route in `project-overview.md` § Routes plus a 404 catch-all; paths centralized in `routes/paths.ts`; `/` redirects to `/dashboard` until auth-aware redirects land.
- **FND-05: React Layout** (2026-10-16) — Ant Design `PublicLayout` (auth pages) and `AuthenticatedLayout` (sidebar, header with placeholder profile menu, content) as nested router layouts; sidebar becomes a drawer below `lg`; active nav via `useActiveNavKey` (nested case routes highlight My Cases); shared theme in `theme/theme.ts`.
- **FND-06: Testing Infrastructure** (2026-10-16) — Vitest (jsdom) + React Testing Library + MSW 3 (unhandled requests fail) with `renderWithProviders` and `src/test/setup.ts`; backend JUnit 5 + Mockito (loaded as `-javaagent`) and Testcontainers PostgreSQL 17 via `@ServiceConnection` (`TestcontainersConfiguration`); sample tests on both sides; overview in `docs/testing-infrastructure.md`.
- **FND-07: Basic CI Pipeline** (2026-10-16) — GitHub Actions "Build & Test" workflow (`.github/workflows/ci.yml`) on every push to `main`: parallel frontend (Node from `frontend/.nvmrc` = 22; `npm ci`, lint, test, build) and backend (Temurin Java 21; `./mvnw verify` with Testcontainers) jobs, npm/Maven caching, read-only token, superseded runs cancelled.
- **AUTH-00-01: CSRF Protection** (2026-10-16) — Spring Security + Spring Session JDBC (sessions in PostgreSQL, `SESSION` cookie HttpOnly/Secure/SameSite=Lax); CSRF tokens stored in the session and sent via `X-XSRF-TOKEN`; public `GET /api/v1/auth/csrf` returns token and header name; Problem Details 403 `FORBIDDEN` for missing/invalid tokens and 401 `UNAUTHORIZED` for unauthenticated requests; all non-public endpoints require auth; flow documented in `docs/csrf-protection.md`.
- **AUTH-01-01: Registration API** (2026-10-16) — Public, CSRF-protected `POST /api/v1/auth/register` creating a `users` + `client_profiles` row (BCrypt hash, email trimmed/lowercased, case-insensitive unique index on `lower(email)`); returns 201 without logging in; 400 `VALIDATION_ERROR` with `fieldErrors` (password 12+ chars, ≤72 bytes) and 409 `EMAIL_ALREADY_EXISTS` via `ApiExceptionHandler`/`ApiException`; Problem Details always include `type`; Flyway introduced (V1 Spring Session tables, V2 users/profiles, `ddl-auto: validate`, see `docs/flyway-migrations.md`); backend moved to 4-space indentation and Lombok.
- **AUTH-01-02: Registration Form** (2026-10-16) — Ant Design form on `/register` (first/last name, email, password, confirm) with client-side validation (required, email format, password 12+ chars and ≤72 bytes, matching passwords, API length limits); Fetch API client (`api/client.ts`) fetches a CSRF token before each POST and throws a typed `ApiError` from Problem Details; API `fieldErrors` and 409 `EMAIL_ALREADY_EXISTS` shown under fields, other errors in an alert; form disabled/loading while submitting; 201 redirects to `/login`; Vite dev proxy `/api` → `localhost:8080`; RTL + MSW tests; `docs/typescript-runtime-types.md`.
- **AUTH-02-01: Login API** (2026-10-16) — Public, CSRF-protected `POST /api/v1/auth/login` authenticating email (trimmed/lowercased) + BCrypt password via a `DaoAuthenticationProvider`/`ClientUserDetailsService`; 200 returns `{ id, email, firstName, lastName }`; `SessionLogin` changes the session ID, replaces the CSRF token and saves the `SecurityContext` to the Spring Session JDBC session; same generic 401 `UNAUTHORIZED` for unknown email, wrong password, suspended/disabled/deleted accounts (status checked after the password); in-memory `LoginAttemptLimiter` (5 failures/email, 50/IP per 15 min, `app.security.login-rate-limit.*`) returns 429 `RATE_LIMIT_EXCEEDED`; `server.forward-headers-strategy: native` for client IPs behind the ALB; flow in `docs/session-login.md`.
- **AUTH-02-02: Login Form** (2026-10-16) — Ant Design form on `/login` (email, password) with required/email-format validation; `login()` posts to `POST /api/v1/auth/login` via `api/client.ts` (CSRF token fetched first, sent in `X-XSRF-TOKEN`); signed-in client kept in React state via `AuthProvider`/`AuthContext` and `useAuth`, set by `useLogin`; every 401 shows the generic "Invalid email or password." alert, API `fieldErrors` go under fields, other errors (e.g. 429) in an alert; form disabled/loading while submitting; success redirects to `/dashboard`; `renderWithProviders` wraps `AuthProvider`; RTL + MSW tests. User state is lost on refresh until session restoration (`GET /auth/me`) lands.
- **AUTH-03-01: Session Restoration** (2026-10-16) — `GET /api/v1/auth/me` returns 200 `{ id, email, firstName, lastName }` (`AuthenticatedClientResponse`, shared with login) for an authenticated session, reloading user and profile from the database; missing, unknown, anonymous or expired sessions get 401 `UNAUTHORIZED` "Authentication required." from the security entry point; accounts deleted, suspended or disabled after login have their session ended (`SessionLogin.end`) and get the same 401; `AuthProvider` restores the client via `fetchCurrentUser()` on start-up behind a full-screen `Spin`, any failure leaves them signed out; profile menu shows the client's name; `renderWithProviders` takes a `user` option that skips the check; `CurrentClientIntegrationTest`, `AuthProvider`/`ProfileMenu` tests; flow in `docs/session-restoration.md`.
