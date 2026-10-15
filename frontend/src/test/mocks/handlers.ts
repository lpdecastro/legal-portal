import type { RequestHandler } from 'msw'

// Default handlers shared by every test. Override per test with `server.use(...)`.
export const handlers: RequestHandler[] = []
