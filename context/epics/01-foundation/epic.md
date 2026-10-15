# Epic: Foundation

**Epic ID:** FND  
**Status:** Not Started

## Objective

Establish the frontend, backend, database, testing, and CI foundations required to develop the LegalMatch-style Client Dashboard.

## Scope

- Initialize React + Vite + TypeScript frontend.
- Initialize Java 21 + Spring Boot backend.
- Configure PostgreSQL for local development.
- Set up frontend routing and shared layouts using Ant Design.
- Configure frontend and backend testing infrastructure.
- Set up GitHub Actions for automated checks on every push to `main`.

## Tasks

| ID     | Task                   | Status      |
| ------ | ---------------------- | ----------- |
| FND-01 | Frontend Setup         | Not Started |
| FND-02 | Backend Setup          | Not Started |
| FND-03 | PostgreSQL Setup       | Not Started |
| FND-04 | React Routing          | Not Started |
| FND-05 | React Layout           | Not Started |
| FND-06 | Testing Infrastructure | Not Started |
| FND-07 | Basic CI Pipeline      | Not Started |

## Acceptance Criteria

1. Frontend application starts locally using Vite.
2. Backend application starts locally and exposes a working health endpoint.
3. Backend successfully connects to PostgreSQL running through Docker Compose.
4. Frontend navigation works across placeholder pages without full-page reloads.
5. Public and authenticated layouts render correctly on desktop and mobile.
6. Frontend and backend automated tests execute successfully.
7. GitHub Actions automatically runs build and test checks on every push to `main`.
8. CI reports failure when any required check fails.

## Out of Scope

- Registration, login, and session management.
- Business APIs and database entities.
- Actual dashboard and case management functionality.
- AWS infrastructure and deployment.

## Definition of Done

- All seven tasks are completed.
- All epic acceptance criteria are satisfied.
- Automated tests and builds pass.
- Changes are committed to the repository.
- The application is ready for Authentication development.
