# FND-07: Basic CI Pipeline

**Status:** Not Started

## Objective

Set up GitHub Actions to automatically validate frontend and backend changes.

## Scope

- Configure GitHub Actions workflow.
- Run CI on every push to `main`.
- Install frontend and backend dependencies.
- Run frontend lint, tests, and build.
- Run backend tests and build.
- Use Java 21 and the project's Node.js version.

## Acceptance Criteria

- Pushing to `main` automatically triggers CI.
- Frontend lint, tests, and build pass.
- Backend tests and build pass.
- CI fails if any required check fails.
- CI results are visible in GitHub Actions.

## Out of Scope

- AWS deployment.
- Infrastructure provisioning.
- Pull request checks.
