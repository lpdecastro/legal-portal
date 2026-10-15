# Current Feature: FND-06 Testing Infrastructure

Set up automated testing for React and Spring Boot.

## Status

In Progress

## Goals

- Configure Vitest and React Testing Library in `frontend/`.
- Configure MSW for mocking API responses.
- Configure JUnit 5 and Mockito in `backend/`.
- Configure Testcontainers with PostgreSQL for integration tests.
- Add sample frontend and backend tests.
- Add test scripts and commands.
- `npm run test` runs frontend tests successfully.
- `./mvnw test` runs backend tests successfully.
- Sample React component test passes.
- Sample Spring Boot test passes.
- Testcontainers successfully starts PostgreSQL for an integration test.
- Tests fail when an assertion is intentionally broken.

## Notes

- Spec: `context/epics/01-foundation/tasks/FND-06-testing.md`.
- Out of scope: authentication and business logic tests, Playwright E2E, CI pipeline configuration.
- Testcontainers requires a running Docker daemon for `./mvnw test`.
- Installed versions are newer than common docs: Vitest 5, MSW 3 (`onUnhandledFrame` replaces `onUnhandledRequest`), jest-dom 7, Testcontainers 2 (`testcontainers-postgresql` artifact, `org.testcontainers.postgresql.PostgreSQLContainer`).
- Frontend: test config in `vite.config.ts`; `src/test/setup.ts` (jest-dom, RTL cleanup, matchMedia stub for antd, MSW lifecycle); `src/test/render.tsx` `renderWithProviders`; samples `Placeholder.test.tsx`, `test/mocks/server.test.ts`.
- Backend: `TestcontainersConfiguration` (`@ServiceConnection` postgres:17-alpine); samples `PostgresContainerIntegrationTest`, `HealthEndpointTest` (MockMvc), `MockitoSetupTest`; surefire loads Mockito as a `-javaagent`.
- Verified: both suites pass; intentionally broken assertions fail; backend passes with no `.env`. No UI changes, so no browser check needed beyond `npm run build`.

## History

<!-- Keep this updated. Earliest to latest -->

- **FND-03: PostgreSQL Setup** (2026-10-15) — Docker Compose PostgreSQL 17 service, Spring Data JPA + PostgreSQL driver, datasource configured from env vars via git-ignored repo-root `.env` (template in `.env.example`).
- **FND-04: React Routing** (2026-10-15) — React Router (`react-router` 8) browser router with placeholder pages for every route in `project-overview.md` § Routes plus a 404 catch-all; paths centralized in `routes/paths.ts`; `/` redirects to `/dashboard` until auth-aware redirects land.
- **FND-05: React Layout** (2026-10-16) — Ant Design `PublicLayout` (auth pages) and `AuthenticatedLayout` (sidebar, header with placeholder profile menu, content) as nested router layouts; sidebar becomes a drawer below `lg`; active nav via `useActiveNavKey` (nested case routes highlight My Cases); shared theme in `theme/theme.ts`.
