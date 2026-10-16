# Current Feature: AUTH-01-01 Registration API

Backend `POST /api/v1/auth/register` endpoint that creates a client account (user + client profile).

## Status

In Progress

## Goals

- `POST /api/v1/auth/register` accepts `firstName`, `lastName`, `email`, `password` (JSON, CSRF-protected via `X-XSRF-TOKEN`)
- Bean Validation: all fields required, valid email format, password minimum 12 characters
- Duplicate emails rejected case-insensitively → 409 `EMAIL_ALREADY_EXISTS` ("Email address is already registered.")
- Invalid input → 400 `VALIDATION_ERROR` Problem Details with `fieldErrors` (`field`, `message`)
- Password stored as a BCrypt hash, never plaintext
- Persist both `users` and `client_profiles` rows in PostgreSQL
- Success → 201 with `{ id, email, firstName, lastName }` (DTO only, never entities)
- Registration does not log the client in (no authenticated session created)
- All errors are RFC 9457 `application/problem+json` with `errorCode`, `timestamp`, `traceId`
- Automated tests (unit + Testcontainers integration) pass

## Notes

- Spec: `context/epics/02-authentication/stories/AUTH-01-registration/tasks/AUTH-01-01-registration-api.md`
- Type: Backend only. Out of scope: registration form, login/session creation, email verification.
- Endpoint must be public (permitted without auth) but still requires a CSRF token — builds on AUTH-00-01 (`GET /api/v1/auth/csrf`, existing Problem Details 401/403 handling).
- Check `context/project-overview.md` for the `users` / `client_profiles` schema, UUID PKs, and error-code list before implementing.
- Example 400 message texts: "Invalid email address", "Password must contain at least 12 characters".
- Implementation decisions:
  - Flyway owns the schema: `V1` Spring Session tables (moved from `initialize-schema: always`, `IF NOT EXISTS` so pre-Flyway DBs migrate), `V2` `users` + `client_profiles`. `baseline-on-migrate` at version 0 for existing local DBs. Hibernate `ddl-auto: validate` (per FND-03 spec). Documented in `docs/flyway-migrations.md`.
  - Enum columns are `varchar` + `CHECK` constraints (mapped with `@Enumerated(STRING)`); email uniqueness is a `lower(email)` unique index, and emails are stored trimmed + lowercased.
  - Password also capped at 72 UTF-8 bytes (`@MaxUtf8Bytes`): BCrypt rejects longer input.
  - Problem Details always include `"type": "about:blank"` (Spring 7 omits it by default).
  - New Java standards applied to the whole backend: 4-space indentation and Lombok (`@Getter`, `@RequiredArgsConstructor`, `@Slf4j`, `@NoArgsConstructor`).

## History

<!-- Keep this updated. Earliest to latest -->

- **FND-03: PostgreSQL Setup** (2026-10-15) — Docker Compose PostgreSQL 17 service, Spring Data JPA + PostgreSQL driver, datasource configured from env vars via git-ignored repo-root `.env` (template in `.env.example`).
- **FND-04: React Routing** (2026-10-15) — React Router (`react-router` 8) browser router with placeholder pages for every route in `project-overview.md` § Routes plus a 404 catch-all; paths centralized in `routes/paths.ts`; `/` redirects to `/dashboard` until auth-aware redirects land.
- **FND-05: React Layout** (2026-10-16) — Ant Design `PublicLayout` (auth pages) and `AuthenticatedLayout` (sidebar, header with placeholder profile menu, content) as nested router layouts; sidebar becomes a drawer below `lg`; active nav via `useActiveNavKey` (nested case routes highlight My Cases); shared theme in `theme/theme.ts`.
- **FND-06: Testing Infrastructure** (2026-10-16) — Vitest (jsdom) + React Testing Library + MSW 3 (unhandled requests fail) with `renderWithProviders` and `src/test/setup.ts`; backend JUnit 5 + Mockito (loaded as `-javaagent`) and Testcontainers PostgreSQL 17 via `@ServiceConnection` (`TestcontainersConfiguration`); sample tests on both sides; overview in `docs/testing-infrastructure.md`.
- **FND-07: Basic CI Pipeline** (2026-10-16) — GitHub Actions "Build & Test" workflow (`.github/workflows/ci.yml`) on every push to `main`: parallel frontend (Node from `frontend/.nvmrc` = 22; `npm ci`, lint, test, build) and backend (Temurin Java 21; `./mvnw verify` with Testcontainers) jobs, npm/Maven caching, read-only token, superseded runs cancelled.
- **AUTH-00-01: CSRF Protection** (2026-10-16) — Spring Security + Spring Session JDBC (sessions in PostgreSQL, `SESSION` cookie HttpOnly/Secure/SameSite=Lax); CSRF tokens stored in the session and sent via `X-XSRF-TOKEN`; public `GET /api/v1/auth/csrf` returns token and header name; Problem Details 403 `FORBIDDEN` for missing/invalid tokens and 401 `UNAUTHORIZED` for unauthenticated requests; all non-public endpoints require auth; flow documented in `docs/csrf-protection.md`.
