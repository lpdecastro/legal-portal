# FND-02: Backend Setup

**Status:** Completed

## Objective

Initialize Java 21 + Spring Boot 4.1.1 in `backend/`.

## Scope

- Set up Spring Boot with Maven.
- Add Spring Web and Actuator and Dev Tools dependencies.
- Configure application properties.
- Expose a health check endpoint.

## Acceptance Criteria

- `./mvnw spring-boot:run` starts the backend locally.
- `GET /actuator/health` returns HTTP 200 with `UP` status.
- `./mvnw clean package` succeeds.

## Out of Scope

- PostgreSQL configuration.
- Authentication and authorization.
- Business APIs.
- Testing infrastructure.
