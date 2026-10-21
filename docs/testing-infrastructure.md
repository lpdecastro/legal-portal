# Testing Infrastructure

How the frontend and backend test setups fit together. Introduced in FND-06.

## Frontend (Vitest + React Testing Library + MSW)

1. `npm run test` runs `vitest run`. Vitest reads the `test` block in [`frontend/vite.config.ts`](../frontend/vite.config.ts) (jsdom environment, `restoreMocks: true`).
2. Before each test file runs, Vitest loads [`frontend/src/test/setup.ts`](../frontend/src/test/setup.ts), which:
   - registers the jest-dom matchers (`toBeInTheDocument`, etc.);
   - stubs `window.matchMedia`, which jsdom doesn't provide and antd's responsive hooks need;
   - starts the MSW server from [`src/test/mocks/server.ts`](../frontend/src/test/mocks/server.ts) with `onUnhandledFrame: 'error'`.
3. The server loads its default handlers from [`src/test/mocks/handlers.ts`](../frontend/src/test/mocks/handlers.ts). This list is empty for now.
4. Tests render components with `renderWithProviders` from [`src/test/render.tsx`](../frontend/src/test/render.tsx). It wraps the component in the app's antd `ConfigProvider` (with the shared theme), antd `App`, and a `MemoryRouter`. Pass `initialEntries` to choose the starting route.
5. A test that needs API data adds a handler for that test with `server.use(http.get(...))`. Any request without a handler fails the test.
6. After each test, RTL `cleanup()` unmounts the DOM and `server.resetHandlers()` removes the handlers that test added. After all tests in the file, the server closes.

Examples: [`src/components/Placeholder.test.tsx`](../frontend/src/components/Placeholder.test.tsx) (component test) and [`src/test/mocks/server.test.ts`](../frontend/src/test/mocks/server.test.ts) (MSW behavior).

## Backend (JUnit 5 + Mockito + Testcontainers)

Docker must be running.

1. `./mvnw test` runs Surefire. The `maven-dependency-plugin` `properties` goal exposes the Mockito jar path, and Surefire's `argLine` uses it to load Mockito with `-javaagent`. Newer JDKs block Mockito from attaching itself at runtime. See [`backend/pom.xml`](../backend/pom.xml).
2. Plain unit tests run without Spring, using `@ExtendWith(MockitoExtension.class)` and `@Mock`. Example: `MockitoSetupTest`.
3. `@SpringBootTest` classes add `@Import(TestcontainersConfiguration.class)`. [`TestcontainersConfiguration`](../backend/src/test/java/com/liandrejohn/legalportal/TestcontainersConfiguration.java) starts a `postgres:17-alpine` container (the same image as Docker Compose).
4. The container bean is annotated `@ServiceConnection`, so Spring Boot points the datasource at the container automatically. No `.env` file is needed for tests.
5. With the database connected, the application context loads and the integration tests run against it:
   - `PostgresContainerIntegrationTest` checks that the container is running and that `JdbcTemplate` is connected to PostgreSQL 17.
   - `HealthEndpointTest` calls `GET /actuator/health` through MockMvc and expects `UP` with no component details.
   - `LegalPortalBackendApplicationTests` checks that the context loads.

## Commands

```bash
# frontend/
npm run test                                        # single pass
npm run test:watch
npx vitest run src/components/Placeholder.test.tsx  # single file

# backend/
./mvnw test
./mvnw test -Dtest=ClassName#methodName
```

## E2E (Playwright)

Introduced in FND-09. Docker must be running.

1. `npm run test:e2e` runs `playwright test` with [`frontend/playwright.config.ts`](../frontend/playwright.config.ts) (Chromium only; 30 s test and 5 s assertion timeouts; 2 retries in CI, none locally).
2. Before the tests, Playwright starts two web servers on their own ports, so E2E never uses the dev servers or the dev database:
   - **Backend** on `8081`: `docker compose -f docker-compose.e2e.yml up -d --wait --force-recreate` recreates the dedicated test database (PostgreSQL 17 on `5433`, data in tmpfs, so every run starts empty and Flyway migrates it), then `./mvnw spring-boot:run` starts the API against it. Ready when `GET /actuator/health` answers.
   - **Frontend** on `5174`: `npm run dev`, with `API_PROXY_TARGET` pointing the `/api` proxy at `8081`.
   - Locally, servers already running on those ports are reused (the database is then not recreated). In CI they are always started.
3. Specs live in [`frontend/e2e/`](../frontend/e2e/) and import `test`/`expect` from [`e2e/fixtures.ts`](../frontend/e2e/fixtures.ts), which fails any test whose page throws an uncaught error. Add shared fixtures there.
4. [`e2e/smoke.spec.ts`](../frontend/e2e/smoke.spec.ts) checks the API answers through the app's proxy and the app loads (signed-out visitors land on `/login`).
5. Results: HTML report in `playwright-report/` (`npx playwright show-report`), screenshots and traces of failed tests in `test-results/` (`npx playwright show-trace <trace.zip>`). Both are git-ignored.
6. CI: the `E2E` job in [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) runs on every push to `main`, fails on any failed test, and uploads the report and failure artifacts.
7. The test database container keeps running after a run; stop it with `docker compose -f docker-compose.e2e.yml down`.

```bash
# frontend/
npm run test:e2e                       # all E2E tests
npm run test:e2e:ui                    # Playwright UI mode
npx playwright test e2e/smoke.spec.ts  # single file
```
