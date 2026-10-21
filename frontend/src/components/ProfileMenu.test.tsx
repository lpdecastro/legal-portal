import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { useNavigate, useRoutes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { routes } from '../routes/router'
import { server } from '../test/mocks/server'
import { renderWithProviders } from '../test/render'
import ProfileMenu from './ProfileMenu'

const USER = { id: 'u1', email: 'john@example.com', firstName: 'John', lastName: 'Doe' }
const CSRF = { token: 'csrf-token', headerName: 'X-XSRF-TOKEN' }

const problem = (status: number, errorCode: string, detail: string) =>
  HttpResponse.json(
    { status, errorCode, detail },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )

function mockLogout(resolver: Parameters<typeof http.post>[1]) {
  server.use(
    http.get('*/api/v1/auth/csrf', () => HttpResponse.json(CSRF)),
    http.post('*/api/v1/auth/logout', resolver),
  )
}

// The app's real route tree, plus a way to try opening a protected page afterwards.
function AppRoutes() {
  const navigate = useNavigate()
  return (
    <>
      <button type="button" onClick={() => navigate('/cases')}>
        Go to My Cases
      </button>
      {useRoutes(routes)}
    </>
  )
}

function renderSignedIn() {
  renderWithProviders(<AppRoutes />, { initialEntries: ['/dashboard'], user: USER })
}

async function clickLogout() {
  const user = userEvent.setup()
  await user.click(await screen.findByRole('button', { name: 'Open profile menu' }))
  await user.click(await screen.findByRole('menuitem', { name: /log out/i }))
  return user
}

const loginHeading = () => screen.findByRole('heading', { name: 'Log in' })

describe('ProfileMenu', () => {
  it("shows the signed-in client's name", () => {
    renderWithProviders(<ProfileMenu />, { user: USER })

    expect(screen.getByText('John Doe')).toBeInTheDocument()
  })

  it('falls back to a generic label when signed out', () => {
    renderWithProviders(<ProfileMenu />)

    expect(screen.getByText('My Account')).toBeInTheDocument()
  })

  describe('logout', () => {
    it('ends the session with the CSRF token, signs the client out and redirects to the login page', async () => {
      let csrfHeader: string | null = null
      mockLogout(({ request }) => {
        csrfHeader = request.headers.get(CSRF.headerName)
        return new HttpResponse(null, { status: 204 })
      })
      renderSignedIn()

      await clickLogout()

      expect(await loginHeading()).toBeInTheDocument()
      expect(csrfHeader).toBe(CSRF.token)
    })

    it('keeps protected pages inaccessible after logout', async () => {
      mockLogout(() => new HttpResponse(null, { status: 204 }))
      renderSignedIn()

      const user = await clickLogout()
      expect(await loginHeading()).toBeInTheDocument()
      await user.click(screen.getByRole('button', { name: 'Go to My Cases' }))

      expect(await loginHeading()).toBeInTheDocument()
      expect(screen.queryByRole('heading', { name: 'My Cases' })).not.toBeInTheDocument()
    })

    it('shows a loading state while logging out', async () => {
      // Held until the spinner is checked, so a slow test run can't miss it
      let respond = () => {}
      const responded = new Promise<void>((resolve) => {
        respond = resolve
      })
      mockLogout(async () => {
        await responded
        return new HttpResponse(null, { status: 204 })
      })
      // The dashboard behind the menu loads while logout is pending
      server.use(
        http.get('*/api/v1/dashboard/summary', () =>
          HttpResponse.json({ statistics: { total: 0, draft: 0, submitted: 0, closed: 0 }, recentCases: [] }),
        ),
      )
      renderSignedIn()

      await clickLogout()

      expect(await screen.findByRole('button', { name: 'Logging out' })).toBeInTheDocument()
      respond()
      expect(await loginHeading()).toBeInTheDocument()
    })

    it.each([
      ['the CSRF token is rejected', () => problem(403, 'FORBIDDEN', 'Access denied.')],
      ['the server fails', () => problem(500, 'INTERNAL_ERROR', 'Something went wrong.')],
      ['the network is down', () => HttpResponse.error()],
    ])('shows an error and keeps the client signed in when %s', async (_, resolver) => {
      mockLogout(resolver)
      renderSignedIn()

      await clickLogout()

      expect(await screen.findByText("We couldn't log you out. Please try again.")).toBeInTheDocument()
      // Still signed in: the protected page stays and the menu can be used to retry.
      expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
      expect(await screen.findByRole('button', { name: 'Open profile menu' })).toBeEnabled()
    })

    it('signs the client out when the session had already ended', async () => {
      mockLogout(() => problem(401, 'UNAUTHORIZED', 'Authentication required.'))
      renderSignedIn()

      await clickLogout()

      expect(await loginHeading()).toBeInTheDocument()
      expect(screen.queryByText("We couldn't log you out. Please try again.")).not.toBeInTheDocument()
    })
  })
})
