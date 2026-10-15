# Current Feature

<!-- Feature name and short description -->

## Status

<!-- Not Started / In Progress / Completed -->

## Goals

<!-- Goals and requirements -->

## Notes

<!-- Any extra notes -->

## History

<!-- Keep this updated. Earliest to latest -->

- **FND-03: PostgreSQL Setup** (2026-10-15) — Docker Compose PostgreSQL 17 service, Spring Data JPA + PostgreSQL driver, datasource configured from env vars via git-ignored repo-root `.env` (template in `.env.example`).
- **FND-04: React Routing** (2026-10-15) — React Router (`react-router` 8) browser router with placeholder pages for every route in `project-overview.md` § Routes plus a 404 catch-all; paths centralized in `routes/paths.ts`; `/` redirects to `/dashboard` until auth-aware redirects land.
- **FND-05: React Layout** (2026-10-16) — Ant Design `PublicLayout` (auth pages) and `AuthenticatedLayout` (sidebar, header with placeholder profile menu, content) as nested router layouts; sidebar becomes a drawer below `lg`; active nav via `useActiveNavKey` (nested case routes highlight My Cases); shared theme in `theme/theme.ts`.
