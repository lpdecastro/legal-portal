import { screen } from '@testing-library/react'
import { http, HttpResponse, delay } from 'msw'
import { describe, expect, it } from 'vitest'
import { useAuth } from '../hooks/useAuth'
import { server } from '../test/mocks/server'
import { renderWithProviders } from '../test/render'
import AuthProvider from './AuthProvider'

const USER = { id: 'u1', email: 'john@example.com', firstName: 'John', lastName: 'Doe' }

function mockMe(resolver: Parameters<typeof http.get>[1]) {
  server.use(http.get('*/api/v1/auth/me', resolver))
}

// Shows who auth state says is signed in.
function CurrentUser() {
  const { user } = useAuth()
  return <p>{user ? `Signed in as ${user.firstName} ${user.lastName}` : 'Signed out'}</p>
}

// Renders a provider that restores the session from GET /auth/me, as the app does on start-up.
function renderRestoring() {
  return renderWithProviders(
    <AuthProvider>
      <CurrentUser />
    </AuthProvider>,
  )
}

describe('AuthProvider', () => {
  it('shows a loading state while checking the session, then restores the client', async () => {
    mockMe(async () => {
      await delay(50)
      return HttpResponse.json(USER)
    })

    renderRestoring()

    expect(screen.getByText('Loading…')).toBeInTheDocument()
    expect(screen.queryByText(/signed/i)).not.toBeInTheDocument()
    expect(await screen.findByText('Signed in as John Doe')).toBeInTheDocument()
    expect(screen.queryByText('Loading…')).not.toBeInTheDocument()
  })

  it('stays signed out when the session is missing, invalid or expired', async () => {
    mockMe(() =>
      HttpResponse.json(
        { status: 401, errorCode: 'UNAUTHORIZED', detail: 'Authentication required.' },
        { status: 401, headers: { 'Content-Type': 'application/problem+json' } },
      ),
    )

    renderRestoring()

    expect(await screen.findByText('Signed out')).toBeInTheDocument()
  })

  it('stays signed out when the session check fails', async () => {
    mockMe(() => HttpResponse.json({ status: 500 }, { status: 500 }))

    renderRestoring()

    expect(await screen.findByText('Signed out')).toBeInTheDocument()
  })

  it('stays signed out when the network is down', async () => {
    mockMe(() => HttpResponse.error())

    renderRestoring()

    expect(await screen.findByText('Signed out')).toBeInTheDocument()
  })

  it('skips the session check when given an initial user', () => {
    // No /auth/me handler: an unexpected request would fail the test
    renderWithProviders(<CurrentUser />, { user: USER })

    expect(screen.getByText('Signed in as John Doe')).toBeInTheDocument()
  })
})
