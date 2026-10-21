import { defineConfig, devices } from '@playwright/test'

const isCI = Boolean(process.env.CI)

// Own ports, so E2E runs don't touch (or clash with) the dev servers on 5173/8080 and their database
const APP_PORT = 5174
const API_PORT = 8081

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  expect: { timeout: 5_000 },
  fullyParallel: true,
  forbidOnly: isCI,
  retries: isCI ? 2 : 0,
  reporter: isCI ? [['github'], ['html', { open: 'never' }]] : [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: `http://localhost:${APP_PORT}`,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      // Recreate the test database (empty, tmpfs) and wait for it to be healthy, then start the backend against it
      command: 'docker compose -f ../docker-compose.e2e.yml up -d --wait --force-recreate && ./mvnw -q spring-boot:run',
      cwd: '../backend',
      url: `http://localhost:${API_PORT}/actuator/health`,
      env: {
        SERVER_PORT: String(API_PORT),
        POSTGRES_HOST: 'localhost',
        POSTGRES_PORT: '5433',
        POSTGRES_DB: 'legal_portal_e2e',
        POSTGRES_USER: 'legal_portal_e2e',
        POSTGRES_PASSWORD: 'legal_portal_e2e',
        SQL_LOG_LEVEL: 'INFO',
      },
      timeout: 180_000,
      reuseExistingServer: !isCI,
    },
    {
      command: `npm run dev -- --port ${APP_PORT} --strictPort`,
      url: `http://localhost:${APP_PORT}`,
      env: { API_PROXY_TARGET: `http://localhost:${API_PORT}` },
      timeout: 60_000,
      reuseExistingServer: !isCI,
    },
  ],
})
