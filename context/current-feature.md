# Current Feature: AUTH-01-02 Registration Form

Allow clients to register an account through the React UI.

## Status

In Progress

## Goals

- Ant Design registration form with first name, last name, email, password, and confirm password.
- Client-side validation: required fields, email format, password minimum 12 characters, matching passwords.
- Fetch `GET /api/v1/auth/csrf` before submitting and send the token in the `X-XSRF-TOKEN` header.
- Submit to `POST /api/v1/auth/register` with the Fetch API, following the AUTH-01-01 contract.
- Display validation errors (including API `fieldErrors`) and API errors; duplicate email (409 `EMAIL_ALREADY_EXISTS`) shows an error message.
- Submit button disabled / loading while submitting.
- Successful registration (201) redirects to `/login`.
- Automated tests (Vitest + RTL + MSW) covering the above, all passing.

## Notes

- Type: Frontend. Spec: `context/epics/02-authentication/stories/AUTH-01-registration/tasks/AUTH-01-02-registration-form.md`.
- API contracts: `AUTH-01-01-registration-api.md` (register) and `AUTH-00-01-csrf-protection.md` (CSRF). Registration does not log the user in; errors are RFC 9457 Problem Details with `errorCode` and `fieldErrors`. Backend also caps password at 72 bytes.
- Session cookie must be sent with both requests (same origin) so the CSRF token matches the session.
- Out of scope: registration API implementation, login and session management, email verification.

## History

<!-- Keep this updated. Earliest to latest -->

- **FND-03: PostgreSQL Setup** (2026-10-15) — Docker Compose PostgreSQL 17 service, Spring Data JPA + PostgreSQL driver, datasource configured from env vars via git-ignored repo-root `.env` (template in `.env.example`).
- **FND-04: React Routing** (2026-10-15) — React Router (`react-router` 8) browser router with placeholder pages for every route in `project-overview.md` § Routes plus a 404 catch-all; paths centralized in `routes/paths.ts`; `/` redirects to `/dashboard` until auth-aware redirects land.
- **FND-05: React Layout** (2026-10-16) — Ant Design `PublicLayout` (auth pages) and `AuthenticatedLayout` (sidebar, header with placeholder profile menu, content) as nested router layouts; sidebar becomes a drawer below `lg`; active nav via `useActiveNavKey` (nested case routes highlight My Cases); shared theme in `theme/theme.ts`.
- **FND-06: Testing Infrastructure** (2026-10-16) — Vitest (jsdom) + React Testing Library + MSW 3 (unhandled requests fail) with `renderWithProviders` and `src/test/setup.ts`; backend JUnit 5 + Mockito (loaded as `-javaagent`) and Testcontainers PostgreSQL 17 via `@ServiceConnection` (`TestcontainersConfiguration`); sample tests on both sides; overview in `docs/testing-infrastructure.md`.
- **FND-07: Basic CI Pipeline** (2026-10-16) — GitHub Actions "Build & Test" workflow (`.github/workflows/ci.yml`) on every push to `main`: parallel frontend (Node from `frontend/.nvmrc` = 22; `npm ci`, lint, test, build) and backend (Temurin Java 21; `./mvnw verify` with Testcontainers) jobs, npm/Maven caching, read-only token, superseded runs cancelled.
- **AUTH-00-01: CSRF Protection** (2026-10-16) — Spring Security + Spring Session JDBC (sessions in PostgreSQL, `SESSION` cookie HttpOnly/Secure/SameSite=Lax); CSRF tokens stored in the session and sent via `X-XSRF-TOKEN`; public `GET /api/v1/auth/csrf` returns token and header name; Problem Details 403 `FORBIDDEN` for missing/invalid tokens and 401 `UNAUTHORIZED` for unauthenticated requests; all non-public endpoints require auth; flow documented in `docs/csrf-protection.md`.
- **AUTH-01-01: Registration API** (2026-10-16) — Public, CSRF-protected `POST /api/v1/auth/register` creating a `users` + `client_profiles` row (BCrypt hash, email trimmed/lowercased, case-insensitive unique index on `lower(email)`); returns 201 without logging in; 400 `VALIDATION_ERROR` with `fieldErrors` (password 12+ chars, ≤72 bytes) and 409 `EMAIL_ALREADY_EXISTS` via `ApiExceptionHandler`/`ApiException`; Problem Details always include `type`; Flyway introduced (V1 Spring Session tables, V2 users/profiles, `ddl-auto: validate`, see `docs/flyway-migrations.md`); backend moved to 4-space indentation and Lombok.
