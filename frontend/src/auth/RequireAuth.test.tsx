import { act, screen } from '@testing-library/react'
import { http, HttpResponse, delay } from 'msw'
import { useRoutes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { getJson } from '../api/client'
import { routes } from '../routes/router'
import { server } from '../test/mocks/server'
import { renderWithProviders } from '../test/render'
import AuthProvider from './AuthProvider'

const USER = { id: 'u1', email: 'john@example.com', firstName: 'John', lastName: 'Doe' }

const UNAUTHORIZED = () =>
  HttpResponse.json(
    { status: 401, errorCode: 'UNAUTHORIZED', detail: 'Authentication required.' },
    { status: 401, headers: { 'Content-Type': 'application/problem+json' } },
  )

// The app's real route tree, rendered inside the test's in-memory router.
function AppRoutes() {
  return useRoutes(routes)
}

function mockMe(resolver: Parameters<typeof http.get>[1]) {
  server.use(http.get('*/api/v1/auth/me', resolver))
}

// Renders the routes behind a provider that restores the session from GET /auth/me, as after a page refresh.
function renderAfterRefresh(path: string) {
  return renderWithProviders(
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>,
    { initialEntries: [path] },
  )
}

const loginHeading = () => screen.findByRole('heading', { name: 'Log in' })

describe('protected routes', () => {
  it.each([
    ['/dashboard', 'Dashboard'],
    ['/cases', 'My Cases'],
    ['/cases/new', 'Create Draft Case'],
    ['/profile', 'Profile'],
  ])('lets a signed-in client open %s', async (path, title) => {
    // My Cases loads the cases; Create Draft loads the legal categories.
    server.use(
      http.get('*/api/v1/cases', () => HttpResponse.json({ content: [], page: { size: 10, number: 0, totalElements: 0, totalPages: 0 } })),
      http.get('*/api/v1/legal-categories', () => HttpResponse.json([])),
    )
    renderWithProviders(<AppRoutes />, { initialEntries: [path], user: USER })

    expect(await screen.findByRole('heading', { name: title })).toBeInTheDocument()
  })

  it.each(['/dashboard', '/cases', '/cases/new', '/cases/abc', '/cases/abc/edit', '/profile', '/settings', '/'])(
    'redirects a signed-out client from %s to the login page',
    async (path) => {
      renderWithProviders(<AppRoutes />, { initialEntries: [path] })

      expect(await loginHeading()).toBeInTheDocument()
    },
  )

  it('keeps a client with a valid session on the page after a refresh, showing only a loader meanwhile', async () => {
    mockMe(async () => {
      await delay(50)
      return HttpResponse.json(USER)
    })
    server.use(http.get('*/api/v1/cases', () => HttpResponse.json({ content: [], page: { size: 10, number: 0, totalElements: 0, totalPages: 0 } })))

    renderAfterRefresh('/cases')

    expect(screen.getByText('Loading…')).toBeInTheDocument()
    expect(screen.queryByRole('heading')).not.toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'My Cases' })).toBeInTheDocument()
  })

  it('redirects to the login page after a refresh when the session is expired or invalid', async () => {
    mockMe(UNAUTHORIZED)

    renderAfterRefresh('/cases')

    expect(await loginHeading()).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'My Cases' })).not.toBeInTheDocument()
  })

  it('redirects to the login page when the session expires while the client is signed in', async () => {
    server.use(http.get('*/api/v1/cases', UNAUTHORIZED))
    renderWithProviders(<AppRoutes />, { initialEntries: ['/dashboard'], user: USER })
    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()

    await act(() => getJson('/cases').catch(() => undefined))

    expect(await loginHeading()).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Dashboard' })).not.toBeInTheDocument()
  })
})
