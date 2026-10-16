# FND-03: PostgreSQL & Flyway Setup

**Status:** Not Started

## Objective

Set up PostgreSQL for local development with Flyway managing database migrations from the start.

## Scope

- Configure PostgreSQL using Docker Compose.
- Add Spring Data JPA, PostgreSQL, and Flyway dependencies.
- Configure database connection using environment variables.
- Configure Flyway for automatic migrations.
- Disable Hibernate schema generation (`ddl-auto: validate`).

## Acceptance Criteria

- `docker compose up -d` starts PostgreSQL.
- `./mvnw spring-boot:run` connects successfully.
- Flyway executes migrations on startup.
- Database credentials are not committed to Git.

## Out of Scope

- Business entities and tables.
- Production database configuration.