# FND-09: Playwright E2E Setup

**Status:** Not Started

## Objective

Set up automated E2E testing using Playwright in `frontend/`.

## Scope

- Install Playwright Test with Chromium.
- Configure base URL, timeouts, and retries.
- Set up `e2e/` directory and reusable fixtures.
- Configure frontend, backend, and dedicated test database.
- Add npm scripts for running tests and UI mode.
- Configure HTML reports, screenshots, and traces on failure.
- Exclude generated test artifacts from Git.
- Update existing GitHub Actions CI to run Playwright tests.

## Acceptance Criteria

- `npm run test:e2e` executes tests successfully.
- `npm run test:e2e:ui` opens Playwright UI mode.
- A smoke test verifies the application loads.
- Tests run against the real backend and test database.
- Test reports and failure artifacts are generated.
- CI runs E2E tests on every push to `main`.
- CI fails if any E2E test fails.

## Out of Scope

- Feature-specific E2E tests.
- Authentication fixtures and test data management.
- Performance and visual regression testing.