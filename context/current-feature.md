# Current Feature: FND-04 React Routing

Set up client-side routing using React Router in `frontend/`.

## Status

In Progress

## Goals

- Install and configure React Router in `frontend/`.
- Create placeholder pages for every route in `project-overview.md` § Routes: Login, Register, Forgot Password, Reset Password, Verify Email, Dashboard, My Cases, Create Case, Case Details, Edit Draft, Profile, and Settings.
- Configure a route for each page; each route displays its corresponding page.
- Add a Not Found (404) page; unknown routes display it.
- Navigation works without full-page reloads.
- Direct URL access and browser refresh work.

## Notes

- Spec: `context/epics/01-foundation/tasks/FND-04-routing.md`.
- Out of scope: page layouts and styling, authentication and protected routes, business functionality.
- Follow `context/coding-standards.md`: one page component per route, no `any`.
- Route paths come from `project-overview.md` § Routes. `/` redirects to `/dashboard` unconditionally for now; the auth-aware redirects (to `/login`, protected routes, post-registration) and the shared case form for `/cases/new` + `/cases/:id/edit` are deferred to the Authentication and Case epics.

## History

<!-- Keep this updated. Earliest to latest -->

- **FND-03: PostgreSQL Setup** (2026-10-15) — Docker Compose PostgreSQL 17 service, Spring Data JPA + PostgreSQL driver, datasource configured from env vars via git-ignored repo-root `.env` (template in `.env.example`).
