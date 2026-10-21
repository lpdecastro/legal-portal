# BKL-07: Feature E2E Tests

**Type:** FE  
**Epic:** BKL – Backlog

## Objective

Cover every built client feature with Playwright E2E tests against the real backend and test database.

## Acceptance Criteria

1. Reusable fixtures: register a unique client per test, signed-in page via saved session state, case factories via the API.
2. Auth: register (validation, duplicate email), login (success, invalid credentials), session survives refresh, protected routes redirect to `/login`, logout.
3. Cases: create draft, edit draft, delete draft, submit (incomplete draft blocked, complete draft submitted), close submitted case, case details per status, another client's case shows "Case not found".
4. My Cases: list, pagination, status filter, title search.
5. Dashboard: statistics and recent cases reflect the client's cases.
6. Profile & Settings: view and edit profile, change password (wrong current password, success, login with the new password).
7. Tests are independent, run in parallel, and pass in CI.

## Dependencies

- FND-09.
