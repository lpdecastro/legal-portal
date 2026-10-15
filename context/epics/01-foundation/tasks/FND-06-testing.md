# FND-06: Testing Infrastructure

**Status:** Not Started

## Objective

Set up automated testing for React and Spring Boot.

## Scope

- Configure Vitest and React Testing Library.
- Configure MSW for mocking API responses.
- Configure JUnit 5 and Mockito.
- Configure Testcontainers with PostgreSQL.
- Add sample frontend and backend tests.
- Add test scripts and commands.

## Acceptance Criteria

- `npm run test` runs frontend tests successfully.
- `./mvnw test` runs backend tests successfully.
- Sample React component test passes.
- Sample Spring Boot test passes.
- Testcontainers successfully starts PostgreSQL for an integration test.
- Tests fail when an assertion is intentionally broken.

## Out of Scope

- Authentication and business logic tests.
- End-to-end testing with Playwright.
- CI pipeline configuration.
