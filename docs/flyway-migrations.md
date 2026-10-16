# Flyway Migrations

How the database schema is created and changed. Introduced in AUTH-01-01.

Flyway owns the schema. Each schema change is a numbered SQL file in [`backend/src/main/resources/db/migration/`](../backend/src/main/resources/db/migration/). Flyway runs automatically when the backend starts and applies only the files the database hasn't seen yet. Hibernate never creates or changes tables (`ddl-auto: validate`). It only checks at startup that the entities match the schema.

## Quick answers

**Do I need to run a Flyway command?**
No. It runs automatically on every backend startup: `./mvnw spring-boot:run`, `./mvnw test`, CI, and ECS. There are no `mvn flyway:*` commands in this project.

**Does it run the SQL on every startup?**
No. Flyway does run on every startup, but each migration file executes only once per database. Flyway records applied files in the `flyway_schema_history` table and skips them afterwards.

**Why not let Hibernate create the tables from entities?**
Hibernate only adds things. It never renames, drops or changes column types. It can't express everything in our schema, such as the `lower(email)` unique index or `CHECK` constraints. It can't seed data, and it changes production with SQL nobody reviewed.

**What happens without Flyway?**
No tables get created. Requests fail with errors like `relation "users" does not exist`, and sessions and CSRF break because `SPRING_SESSION` is missing.

**Do I write the full `CREATE TABLE` again when a table changes?**
No. Write `CREATE TABLE` once, when the table is new. Every later change is only the difference (`ALTER TABLE`, `CREATE INDEX`, `INSERT`, ...) in a new file.

**Can I edit an existing migration?**
Not once it's merged to `main` or applied to any shared database. Write a new migration instead. On your own unmerged branch it's fine, but then reset your local database (see [Commands](#commands)).

**Isn't writing SQL a lot of work?**
Usually a few lines per feature. To get a draft, see [Generating SQL from entities](#generating-sql-from-entities).

**What if I add an entity field but forget the migration?**
Startup fails with a Hibernate schema-validation error that names the missing table or column. That is what `ddl-auto: validate` is for.

**How do I undo a migration?**
Write a new migration that reverses it. The free edition of Flyway has no automatic undo.

**How do I see a table's current structure?**
Look at the database, not the migration files: run `\d <table>` in `psql`, or use your IDE's database view.

## Setup

| Piece | Where | Purpose |
|---|---|---|
| `spring-boot-starter-flyway`, `flyway-database-postgresql` | [`pom.xml`](../backend/pom.xml) | Spring Boot runs Flyway automatically at startup |
| Migration files | `backend/src/main/resources/db/migration/` | The default folder Flyway scans |
| `spring.flyway.baseline-on-migrate: true`, `baseline-version: 0` | [`application.yaml`](../backend/src/main/resources/application.yaml) | Lets databases created before Flyway migrate (see [Baseline](#baseline)) |
| `spring.jpa.hibernate.ddl-auto: validate` | `application.yaml` | Hibernate checks the schema but never changes it |
| `spring.session.jdbc.initialize-schema: never` | `application.yaml` | Spring Session no longer creates its own tables. V1 does. |

Current migrations:

| Version | File | Creates |
|---|---|---|
| 1 | `V1__create_spring_session_tables.sql` | `SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES` |
| 2 | `V2__create_users_and_client_profiles.sql` | `users`, `client_profiles` |

## What happens at startup

1. Spring Boot connects to PostgreSQL using the settings in `.env`.
2. Flyway runs **before** JPA and the rest of the app.
3. If the `flyway_schema_history` table doesn't exist, Flyway creates it.
4. Flyway reads every `V*__*.sql` file, sorted by version.
5. For each file:
   - **Already applied, file unchanged:** skipped.
   - **Already applied, file changed:** startup fails with a checksum mismatch.
   - **Not applied yet:** the SQL runs in a transaction, and Flyway records a row in the history table.
6. If a migration fails, it is rolled back and **the app does not start**.
7. Hibernate validates the entities against the schema (`ddl-auto: validate`). If they don't match, the app does not start.
8. The app starts.

Tests (`./mvnw test`) use a fresh PostgreSQL container from Testcontainers, so every migration runs from scratch on each test run.

## Adding a schema change

1. **Find the next version.** Look at the highest `V<n>` in `db/migration/` and add 1.
2. **Create the file** with this name format: capital `V`, the version, **two underscores**, a snake_case description, `.sql`.
   ```
   V3__create_legal_categories.sql
   ```
3. **Write PostgreSQL SQL for only the change:**
   - New table: `CREATE TABLE ...`
   - Change to an existing table: `ALTER TABLE ...`
   - Reference data: `INSERT ...`
4. **Add or update the JPA entity** so it matches the new schema.
5. **Start the app or run the tests.** Look for `Migrating schema "public" to version "3 - create legal categories"` in the log.
6. **Commit the migration together with the code that uses it.**

### Examples

```sql
-- New table
CREATE TABLE legal_categories (
    id         uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       varchar(100) NOT NULL,
    active     boolean      NOT NULL DEFAULT true,
    created_at timestamptz  NOT NULL DEFAULT now()
);

-- Add a column
ALTER TABLE client_profiles ADD COLUMN date_of_birth date;

-- Change a column type
ALTER TABLE client_profiles ALTER COLUMN phone TYPE varchar(50);

-- Rename a column
ALTER TABLE users RENAME COLUMN status TO account_status;

-- Add an index
CREATE INDEX users_created_at_idx ON users (created_at);
```

One file can hold several related changes. Group what belongs to one feature.

### Rules

- **Never edit a migration that's merged or applied to a shared database.** Write a new one.
- **A new `NOT NULL` column needs a default** if the table already has rows. Either add `DEFAULT ...`, or add the column as nullable, fill it with an `UPDATE`, then `ALTER COLUMN ... SET NOT NULL`, all in the same file.
- **Version numbers must be unique.** If another branch merged the same version first, rename your file to the next free number before merging.
- **Prefer backward-compatible changes.** During a rolling deploy, old containers briefly run against the new schema. Split risky changes, such as renaming a column, into several steps across deploys: add the new column, deploy, then drop the old column later.

## Generating SQL from entities

You don't have to type SQL from scratch:

- **Hibernate script export.** Temporarily add this to `application.yaml`, start the app, copy the parts you need into the new migration and adjust them, then remove the setting:
  ```yaml
  spring:
    jpa:
      properties:
        jakarta.persistence.schema-generation.scripts.action: create
        jakarta.persistence.schema-generation.scripts.create-target: build/schema.sql
  ```
- **JPA Buddy** (built into IntelliJ IDEA Ultimate) compares entities with the database and generates a Flyway migration containing only the difference.

Always review generated SQL. It won't include functional indexes like `lower(email)` or `CHECK` constraints.

## Baseline

The local databases created before Flyway (during AUTH-00-01) already contained the Spring Session tables. By default, Flyway refuses to touch a database that has tables but no history table. With `baseline-on-migrate: true` and `baseline-version: 0`, Flyway records a version 0 baseline row and then applies V1 and V2. V1 uses `CREATE ... IF NOT EXISTS`, so the existing session tables don't cause an error. On a fresh database, such as Testcontainers or a new RDS instance, the baseline settings do nothing.

## Common errors

| Error | Cause | Fix |
|---|---|---|
| `Migration checksum mismatch for migration version N` | An applied migration was edited | Local and unmerged: reset the database. Shared: revert the edit and write a new migration. |
| `Detected resolved migration not applied to database: N` | Two branches used the same version, or a lower version arrived after a higher one was applied | Rename the file to the next free version |
| `Schema-validation: missing table [x]` / `missing column [y]` | An entity has no matching table or column | Add a migration |
| SQL error during migration | Bug in the migration | That migration is rolled back. Fix the SQL and restart. |

## Commands

See which migrations have been applied:

```bash
docker exec -it legal-portal-postgres psql -U <POSTGRES_USER> -d <POSTGRES_DB> \
  -c "select installed_rank, version, description, success from flyway_schema_history order by installed_rank;"
```

Reset the local database. **This deletes all local data**, and Flyway rebuilds the schema on the next startup:

```bash
docker compose down -v   # -v removes the postgres-data volume
docker compose up -d
cd backend && ./mvnw spring-boot:run
```

## Production and CI

- **CI:** `./mvnw verify` runs the migrations against Testcontainers. A broken migration or an entity that doesn't match the schema fails the build.
- **Production (ECS + RDS):** each new container applies pending migrations to RDS before it serves traffic. No manual database step is needed in a deploy.
