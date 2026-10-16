# Current Feature: FND-07 Basic CI Pipeline

Set up GitHub Actions to automatically validate frontend and backend changes.

## Status

In Progress

## Goals

- GitHub Actions workflow that runs on every push to `main`
- Install frontend and backend dependencies
- Run frontend lint, tests, and build
- Run backend tests and build
- Use Java 21 and the project's Node.js version
- CI fails if any required check fails; results visible in GitHub Actions

## Notes

- Out of scope: AWS deployment, infrastructure provisioning, pull request checks.
- Node pinned to 22 via `frontend/.nvmrc` (matches local v22; Vite 8 needs 20.19+/22.12+); CI reads it with `node-version-file`.
- Backend tests use Testcontainers (PostgreSQL 17), so the CI runner needs Docker (available on `ubuntu-latest`).
- Frontend commands run from `frontend/` (`npm ci`, `npm run lint`, `npm run test`, `npm run build`); backend from `backend/` via `./mvnw` (Java 21).

## History

<!-- Keep this updated. Earliest to latest -->

- **FND-03: PostgreSQL Setup** (2026-10-15) — Docker Compose PostgreSQL 17 service, Spring Data JPA + PostgreSQL driver, datasource configured from env vars via git-ignored repo-root `.env` (template in `.env.example`).
- **FND-04: React Routing** (2026-10-15) — React Router (`react-router` 8) browser router with placeholder pages for every route in `project-overview.md` § Routes plus a 404 catch-all; paths centralized in `routes/paths.ts`; `/` redirects to `/dashboard` until auth-aware redirects land.
- **FND-05: React Layout** (2026-10-16) — Ant Design `PublicLayout` (auth pages) and `AuthenticatedLayout` (sidebar, header with placeholder profile menu, content) as nested router layouts; sidebar becomes a drawer below `lg`; active nav via `useActiveNavKey` (nested case routes highlight My Cases); shared theme in `theme/theme.ts`.
- **FND-06: Testing Infrastructure** (2026-10-16) — Vitest (jsdom) + React Testing Library + MSW 3 (unhandled requests fail) with `renderWithProviders` and `src/test/setup.ts`; backend JUnit 5 + Mockito (loaded as `-javaagent`) and Testcontainers PostgreSQL 17 via `@ServiceConnection` (`TestcontainersConfiguration`); sample tests on both sides; overview in `docs/testing-infrastructure.md`.
