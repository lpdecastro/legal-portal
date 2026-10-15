# FND-03: PostgreSQL Setup

**Status:** Not Started

## Objective

Set up PostgreSQL for local development and connect it to Spring Boot.

## Scope

- Configure PostgreSQL using Docker Compose.
- Add Spring Data JPA and PostgreSQL driver dependencies.
- Configure Spring Boot database connection.
- Use environment variables for database credentials.

## Acceptance Criteria

- `docker compose up -d` starts PostgreSQL.
- Spring Boot connects to PostgreSQL successfully.
- `./mvnw spring-boot:run` starts without database connection errors.
- Database credentials are not committed to Git.

## Out of Scope

- Business entities and tables.
- Database migrations.
- Production database configuration.
