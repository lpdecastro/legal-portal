# Legal Portal

A case management dashboard for legal clients. Clients can register, manage their profile, and create, submit, track and close their own legal cases.

## Tech Stack

- **Frontend:** React 19, TypeScript, Vite, Ant Design
- **Backend:** Spring Boot (Java 21)
- **Database:** PostgreSQL

## Getting Started

### Database

```bash
cp .env.example .env   # then set POSTGRES_PASSWORD
docker compose up -d
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

API docs: http://localhost:8080/scalar

#### IntelliJ IDEA gotchas

- **Maven fails to run or import:** go to **Settings → Build, Execution, Deployment → Build Tools → Maven** and set **Maven home path** to **Bundled (Maven 3)**.
- **Lombok code doesn't compile (e.g. `cannot find symbol: variable log` from `@Slf4j`):** go to **Settings → Build, Execution, Deployment → Compiler → Annotation Processors**, select the **legal-portal-backend** profile, tick **Enable annotation processing**, and choose **Obtain processors from project classpath**.

## Project Structure

```
frontend/   React app
backend/    Spring Boot API
context/    Project specs and documentation
```
