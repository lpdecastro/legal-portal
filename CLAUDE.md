# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Client-only legal case management dashboard (MVP): clients register, manage a profile, create/edit draft cases, submit, view/filter, and close their own cases. Lawyer accounts, messaging, payments, uploads, and admin are out of scope.

The repo is at an early scaffold stage — `frontend/` is the stock Vite template and `backend/` is a bare Spring Boot app. Most of the intended design lives in `context/`, and that is the source of truth:

- `context/project-overview.md` — business rules, data model, API design, auth model, error format, testing and infra plans. Read this before implementing anything.
- `context/coding-standards.md` — TypeScript/React rules.
- `context/ai-interaction.md` — the per-feature workflow.
- `context/epics/NN-<epic>/` — `epic.md`, then `stories/<ID>/story.md` and `tasks/<ID>.md` specs (e.g. `FND-02-backend-setup.md`, `CASE-02-...`). Epic order: Foundation → Authentication → Case Management → Client Dashboard → Profile & Settings.
- `context/current-feature.md` — the active feature (goals, notes, append-only History).

## Feature workflow

Work is driven by the project skill `/feature` (`.claude/skills/feature/`):

- `/feature load <task-id>` — loads `context/epics/**/tasks/<task-id>.md` (or an inline description) into `current-feature.md`.
- `/feature start` — sets status In Progress, creates a feature branch from the H1 name, implements goals.
- `/feature review` — checks goals met, quality, and scope creep.
- `/feature complete` — commit, merge into `main` locally, delete branch, reset `current-feature.md` (append to History), then push `main` once.

Per `ai-interaction.md`: verify in the browser before committing, commit only after the build passes. The `complete` action says never put "Generated With Claude" in commit messages. A Playwright MCP server is configured in `.mcp.json` for browser verification.

## Commands

Frontend (`cd frontend`):

```bash
npm run dev       # Vite dev server
npm run build     # tsc -b && vite build (type-check + bundle)
npm run lint      # eslint .
npm run test      # vitest run (single pass)
npm run test:watch
npx vitest run src/components/Placeholder.test.tsx   # single file
npm run preview
```

Backend (`cd backend`, Maven wrapper, Java 21):

```bash
./mvnw spring-boot:run
./mvnw test                               # needs Docker running (Testcontainers)
./mvnw test -Dtest=ClassName#methodName   # single test
./mvnw package
```

Frontend tests use Vitest (jsdom) + React Testing Library + MSW, configured in `vite.config.ts` with setup in `src/test/setup.ts`. Render components with `renderWithProviders` (`src/test/render.tsx`) and mock API calls with `server.use(...)` from `src/test/mocks/server.ts`; unhandled requests fail the test (MSW 3: `onUnhandledFrame: 'error'`). Backend integration tests add `@Import(TestcontainersConfiguration.class)` to get a PostgreSQL 17 container via `@ServiceConnection`.

## Architecture (target)

- **Frontend:** React 19 + TypeScript + Vite + Ant Design (antd 6). Public layout (login/register/forgot/reset) and authenticated layout (sidebar, header with profile menu). One page component per route; reusable logic in custom hooks. No `any`. Note: `tsconfig.app.json` does not yet set `"strict": true`, though the standards require strict mode.
- **Backend:** Spring Boot 4.1 (webmvc) acting as a BFF, package `com.liandrejohn.legalportal`. APIs under `/api/v1`. Return DTOs only, never entities. Bean Validation via `@Valid`; pagination via Spring Data `Pageable` with a server-enforced max page size.
- **Auth:** session-based Spring Security with Spring Session JDBC (no JWT). HttpOnly/Secure/SameSite cookies, CSRF on state-changing requests, BCrypt, session restore via `GET /auth/me`, generic login errors, rate-limited auth endpoints. SPA and API share a single origin (CloudFront: `/*` → S3, `/api/*` → ALB → ECS).
- **Authorization:** every case endpoint must verify the case belongs to the current client; inaccessible cases return 404 `CASE_NOT_FOUND`.
- **Errors:** RFC 9457 Problem Details (`application/problem+json`) with custom extensions `errorCode`, `timestamp`, `traceId`, `fieldErrors`. Error codes are listed in `project-overview.md`.
- **Data:** PostgreSQL (Docker Compose locally); UUID PKs; tables `users`, `client_profiles`, `legal_categories`, `cases`. Cases reference `client_profiles.id`, not `users.id`.
- **Case state machine:** `DRAFT` → `SUBMITTED` → `CLOSED`. Only drafts are editable/deletable; submission requires title, category, description, and location; closed cases cannot be reopened; deletes are soft (`deleted_at`); inactive categories cannot be selected. Invalid transitions return 409 `INVALID_CASE_STATE`.
- **Backend testing:** JUnit 5 + Mockito, Spring Boot integration tests with Testcontainers PostgreSQL. E2E via Playwright.
- **Deploy:** GitHub Actions CI on push to `main`; AWS ECS Fargate + RDS + CloudFront/S3.
