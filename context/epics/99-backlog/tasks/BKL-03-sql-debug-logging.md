# BKL-03: SQL Debug Logging

**Type:** BE  
**Epic:** BKL – Backlog

## Objective

Turn on Hibernate SQL logging (`org.hibernate.SQL` at `DEBUG`) from `.env` for local debugging, e.g. to check for N+1 queries.

## Acceptance Criteria

1. An env var (e.g. `SQL_LOG_LEVEL`) sets the `org.hibernate.SQL` log level.
2. Default is the production setting: SQL not logged.
3. Setting it to `DEBUG` in `.env` logs each executed SQL statement after a restart.
4. The variable is documented in `.env.example` with its default.

## Dependencies

- FND-03.
