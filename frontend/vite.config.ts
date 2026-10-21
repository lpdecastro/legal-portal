import react from '@vitejs/plugin-react'
import { configDefaults, defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Keep SPA and API on one origin in development, as CloudFront does in production.
    // API_PROXY_TARGET points the E2E dev server at the E2E backend (playwright.config.ts).
    proxy: {
      '/api': process.env.API_PROXY_TARGET ?? 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    restoreMocks: true,
    // Playwright specs (npm run test:e2e)
    exclude: [...configDefaults.exclude, 'e2e/**'],
  },
})
