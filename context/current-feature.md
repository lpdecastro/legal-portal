# Current Feature: AUTH-00-01 CSRF Protection

Protect state-changing API requests against CSRF attacks using Spring Security, with a token endpoint for the SPA.

## Status

In Progress

## Goals

- Configure CSRF protection with Spring Security, storing tokens server-side in the HTTP session (not a cookie repository).
- Implement `GET /api/v1/auth/csrf` (no authentication required) returning `200` with JSON body `{ "token": "<value>", "headerName": "X-XSRF-TOKEN" }`.
- The endpoint establishes/uses the session: `Set-Cookie: SESSION=<id>; HttpOnly; Secure; SameSite=Lax; Path=/`.
- Clients send the token in the `X-XSRF-TOKEN` header; valid tokens let state-changing requests proceed to normal authentication and validation.
- Missing or invalid CSRF tokens return `403` as RFC 9457 Problem Details (`application/problem+json`) with `title: "Forbidden"`, `detail: "Invalid or missing CSRF token."`, `errorCode: "FORBIDDEN"`, `timestamp`, `traceId`.
- CSRF tokens are bound to the correct session (a token from one session is rejected in another).
- Automated tests cover: unauthenticated token retrieval, valid token accepted, missing token → 403, invalid token → 403, cross-session token → 403; all tests pass.

## Notes

- Spec: `context/epics/02-authentication/tasks/AUTH-00-01-csrf-protection.md`. Type: Backend only.
- Auth model per `project-overview.md`: session-based Spring Security with Spring Session JDBC (no JWT); SPA and API share one origin.
- Out of scope: registration/login APIs, frontend CSRF retrieval/integration, authentication & authorization, session restoration and logout.
- Error format must match the project's Problem Details shape (`errorCode`, `timestamp`, `traceId` extensions).
- Implementation: Spring Session JDBC added (schema via `spring.session.jdbc.initialize-schema: always`); cookie attributes via `server.servlet.session.cookie.*`. Non-public requests require authentication, so a 401 Problem Details entry point was added to keep "not logged in" distinct from the CSRF 403.
- Spring omits `"type"` when it is `about:blank` (equivalent per RFC 9457), so 403 bodies don't include it explicitly.
- Session cookie attributes aren't applied in MockMvc's mock environment, so `SessionCookieTest` checks them against a real server.

## History

<!-- Keep this updated. Earliest to latest -->

- **FND-03: PostgreSQL Setup** (2026-10-15) — Docker Compose PostgreSQL 17 service, Spring Data JPA + PostgreSQL driver, datasource configured from env vars via git-ignored repo-root `.env` (template in `.env.example`).
- **FND-04: React Routing** (2026-10-15) — React Router (`react-router` 8) browser router with placeholder pages for every route in `project-overview.md` § Routes plus a 404 catch-all; paths centralized in `routes/paths.ts`; `/` redirects to `/dashboard` until auth-aware redirects land.
- **FND-05: React Layout** (2026-10-16) — Ant Design `PublicLayout` (auth pages) and `AuthenticatedLayout` (sidebar, header with placeholder profile menu, content) as nested router layouts; sidebar becomes a drawer below `lg`; active nav via `useActiveNavKey` (nested case routes highlight My Cases); shared theme in `theme/theme.ts`.
- **FND-06: Testing Infrastructure** (2026-10-16) — Vitest (jsdom) + React Testing Library + MSW 3 (unhandled requests fail) with `renderWithProviders` and `src/test/setup.ts`; backend JUnit 5 + Mockito (loaded as `-javaagent`) and Testcontainers PostgreSQL 17 via `@ServiceConnection` (`TestcontainersConfiguration`); sample tests on both sides; overview in `docs/testing-infrastructure.md`.
- **FND-07: Basic CI Pipeline** (2026-10-16) — GitHub Actions "Build & Test" workflow (`.github/workflows/ci.yml`) on every push to `main`: parallel frontend (Node from `frontend/.nvmrc` = 22; `npm ci`, lint, test, build) and backend (Temurin Java 21; `./mvnw verify` with Testcontainers) jobs, npm/Maven caching, read-only token, superseded runs cancelled.
