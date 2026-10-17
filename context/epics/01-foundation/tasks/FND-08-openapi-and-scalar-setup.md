# FND-08: OpenAPI and Scalar Setup

**Status:** Not Started

## Objective

Set up API documentation in `backend/` using OpenAPI and Scalar.

## Scope

- Add Springdoc OpenAPI with Scalar.
- Configure API title and version.
- Configure `OPENAPI_ENABLED` and `SCALAR_ENABLED` in `application.yaml` using environment variables.

## Acceptance Criteria

- `/scalar` loads the API documentation UI locally.
- `/v3/api-docs` returns valid OpenAPI JSON with the configured title and version.
- Setting both environment variables to `false` disables the documentation endpoints after restarting the backend.

## Out of Scope

- Business APIs.
- Postman collections.
