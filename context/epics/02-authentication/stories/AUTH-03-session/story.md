# AUTH-03: Session Management

**Status:** Not Started

## User Story

As a logged-in client, I want my session to persist across page refreshes so I can continue using the dashboard without logging in again.

## Acceptance Criteria

- Clients remain authenticated after refreshing the page.
- Authenticated clients can access protected pages.
- Unauthenticated clients are redirected to `/login`.
- Expired or invalid sessions require clients to log in again.
- Session validation is handled by the backend.

## Tasks

- AUTH-03-01: Session Restoration (BE + FE)
- AUTH-03-02: Protected Routes (FE)

## Definition of Done

- Both tasks are completed.
- Session restoration and protected routes work end-to-end.
- Automated tests pass.
