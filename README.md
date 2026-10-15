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

## Project Structure

```
frontend/   React app
backend/    Spring Boot API
context/    Project specs and documentation
```
