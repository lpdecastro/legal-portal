# Current Feature: FND-05 React Layout

Shared public and authenticated layouts using Ant Design in `frontend/`.

## Status

In Progress

## Goals

- Public layout used by Login and Register pages.
- Authenticated layout with sidebar, header, and main content area used by Dashboard, My Cases, Case Details, Submit Case, and Profile pages.
- Sidebar navigation links to the existing routes and works across pages.
- Active navigation item is highlighted (including nested routes, e.g. `/cases/:id` highlights My Cases).
- Layouts adapt to desktop and mobile screens.
- Consistent typography, colors, and spacing applied (Ant Design theme tokens).

## Notes

- Spec: `context/epics/01-foundation/tasks/FND-05-layout.md`.
- Out of scope: authentication and protected routes, actual page content/business functionality, API integration.
- Routes already exist from FND-04 in `frontend/src/routes/paths.ts` / `router.tsx`; use nested layout routes (`<Outlet />`) rather than wrapping each page.
- The remaining public pages (Forgot Password, Reset Password, Verify Email) should also sit under the public layout; Create Case ("Submit Case"), Edit Case, and Settings under the authenticated layout.
- Header profile menu is a placeholder only (no logout/auth wiring yet).
- Stack: antd 6, react-router 8. Follow `context/coding-standards.md` (no `any`, one component per file, hooks for reusable logic).
- Verify in the browser (desktop + mobile widths) before committing; build must pass.

## History

<!-- Keep this updated. Earliest to latest -->

- **FND-03: PostgreSQL Setup** (2026-10-15) — Docker Compose PostgreSQL 17 service, Spring Data JPA + PostgreSQL driver, datasource configured from env vars via git-ignored repo-root `.env` (template in `.env.example`).
- **FND-04: React Routing** (2026-10-15) — React Router (`react-router` 8) browser router with placeholder pages for every route in `project-overview.md` § Routes plus a 404 catch-all; paths centralized in `routes/paths.ts`; `/` redirects to `/dashboard` until auth-aware redirects land.
