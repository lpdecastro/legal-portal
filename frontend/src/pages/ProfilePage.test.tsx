import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse, delay } from 'msw'
import { useRoutes } from 'react-router'
import { describe, expect, it } from 'vitest'
import type { ClientProfile } from '../api/profile'
import { useAuth } from '../hooks/useAuth'
import { routes } from '../routes/router'
import { paths } from '../routes/paths'
import { server } from '../test/mocks/server'
import { renderWithProviders } from '../test/render'

const USER = { id: 'u1', email: 'john@example.com', firstName: 'John', lastName: 'Doe' }
const CSRF_TOKEN = 'csrf-token-123'

const PROFILE: ClientProfile = { firstName: 'John', lastName: 'Doe', email: 'john@example.com', phone: '+63 912 345 6789' }

const problemHeaders = { 'Content-Type': 'application/problem+json' }

const problem = (status: number, errorCode: string, detail: string) =>
  HttpResponse.json({ status, errorCode, detail }, { status, headers: problemHeaders })

type Resolver = Parameters<typeof http.get>[1]

function mockApi({ get = () => HttpResponse.json(PROFILE), patch }: { get?: Resolver; patch?: Resolver } = {}) {
  server.use(
    http.get('*/api/v1/auth/csrf', () => HttpResponse.json({ token: CSRF_TOKEN, headerName: 'X-XSRF-TOKEN' })),
    http.get('*/api/v1/clients/me', get),
  )
  if (patch) {
    server.use(http.patch('*/api/v1/clients/me', patch))
  }
}

// The app's real route tree, plus the signed-in client's name (the header hides it below desktop widths).
function AppRoutes() {
  const { user } = useAuth()
  return (
    <>
      <output aria-label="Signed in as">{user && `${user.firstName} ${user.lastName}`}</output>
      {useRoutes(routes)}
    </>
  )
}

function renderPage() {
  return renderWithProviders(<AppRoutes />, { initialEntries: [paths.profile], user: USER })
}

const saveButton = () => screen.getByRole('button', { name: /save/i })
const cancelButton = () => screen.getByRole('button', { name: 'Cancel' })

// Waits for the profile to load into the form.
async function waitForForm() {
  expect(await screen.findByLabelText('First name')).toHaveValue('John')
  return userEvent.setup()
}

describe('ProfilePage', () => {
  it('shows a loading indicator while the profile loads', async () => {
    mockApi({
      get: async () => {
        await delay('infinite')
        return HttpResponse.json(PROFILE)
      },
    })
    renderPage()

    expect(await screen.findByText('Loading profile…')).toBeInTheDocument()
    expect(screen.queryByLabelText('First name')).not.toBeInTheDocument()
  })

  it('shows the profile with the email read-only', async () => {
    mockApi()
    renderPage()

    await waitForForm()
    expect(screen.getByRole('heading', { name: 'Profile' })).toBeInTheDocument()
    expect(screen.getByLabelText('Last name')).toHaveValue('Doe')
    expect(screen.getByLabelText('Phone number')).toHaveValue('+63 912 345 6789')
    expect(screen.getByLabelText('Email')).toHaveValue('john@example.com')
    expect(screen.getByLabelText('Email')).toHaveAttribute('readonly')
    expect(saveButton()).toBeEnabled()
    expect(cancelButton()).toBeEnabled()
  })

  it('shows an empty phone number when none is set', async () => {
    mockApi({ get: () => HttpResponse.json({ ...PROFILE, phone: null }) })
    renderPage()

    await waitForForm()
    expect(screen.getByLabelText('Phone number')).toHaveValue('')
  })

  it('sends only the changed fields, trimmed, with the CSRF token and shows the saved profile', async () => {
    let received: { body: unknown; token: string | null } | undefined
    mockApi({
      patch: async ({ request }) => {
        received = { body: await request.json(), token: request.headers.get('X-XSRF-TOKEN') }
        return HttpResponse.json({ ...PROFILE, firstName: 'Johnny', phone: '+63 (2) 8123-4567' })
      },
    })
    renderPage()
    const user = await waitForForm()

    await user.clear(screen.getByLabelText('First name'))
    await user.type(screen.getByLabelText('First name'), '  Johnny  ')
    await user.clear(screen.getByLabelText('Phone number'))
    await user.type(screen.getByLabelText('Phone number'), '+63 (2) 8123-4567')
    await user.click(saveButton())

    expect(await screen.findByText('Profile updated.')).toBeInTheDocument()
    expect(received).toEqual({ token: CSRF_TOKEN, body: { firstName: 'Johnny', phone: '+63 (2) 8123-4567' } })
    expect(screen.getByLabelText('First name')).toHaveValue('Johnny')
    // The profile menu reads the signed-in client's name from auth state.
    expect(screen.getByLabelText('Signed in as')).toHaveTextContent('Johnny Doe')
  })

  it('sends null for a cleared phone number', async () => {
    let body: unknown
    mockApi({
      patch: async ({ request }) => {
        body = await request.json()
        return HttpResponse.json({ ...PROFILE, phone: null })
      },
    })
    renderPage()
    const user = await waitForForm()

    await user.clear(screen.getByLabelText('Phone number'))
    await user.type(screen.getByLabelText('Phone number'), '   ')
    await user.click(saveButton())

    expect(await screen.findByText('Profile updated.')).toBeInTheDocument()
    expect(body).toEqual({ phone: null })
  })

  it('skips the request when nothing changed', async () => {
    let requests = 0
    mockApi({
      patch: () => {
        requests += 1
        return HttpResponse.json(PROFILE)
      },
    })
    renderPage()
    const user = await waitForForm()

    await user.type(screen.getByLabelText('First name'), ' ')
    await user.click(saveButton())

    expect(await screen.findByText('No changes to save.')).toBeInTheDocument()
    expect(requests).toBe(0)
  })

  it('restores the loaded values and clears errors on cancel', async () => {
    mockApi()
    renderPage()
    const user = await waitForForm()

    await user.clear(screen.getByLabelText('First name'))
    await user.type(screen.getByLabelText('Phone number'), 'abc')
    await user.click(saveButton())
    expect(await screen.findByText('First name is required')).toBeInTheDocument()

    await user.click(cancelButton())

    expect(screen.getByLabelText('First name')).toHaveValue('John')
    expect(screen.getByLabelText('Phone number')).toHaveValue('+63 912 345 6789')
    await waitFor(() => expect(screen.queryByText('First name is required')).not.toBeInTheDocument())
  })

  it('validates required fields, lengths and the phone format', async () => {
    let requests = 0
    mockApi({
      patch: () => {
        requests += 1
        return HttpResponse.json(PROFILE)
      },
    })
    renderPage()
    const user = await waitForForm()

    await user.clear(screen.getByLabelText('First name'))
    await user.type(screen.getByLabelText('First name'), '   ')
    await user.clear(screen.getByLabelText('Last name'))
    await user.click(saveButton())

    expect(await screen.findByText('First name is required')).toBeInTheDocument()
    expect(screen.getByText('Last name is required')).toBeInTheDocument()

    await user.clear(screen.getByLabelText('First name'))
    await user.click(screen.getByLabelText('First name'))
    await user.paste('a'.repeat(101))
    await user.click(screen.getByLabelText('Last name'))
    await user.paste('b'.repeat(101))
    await user.clear(screen.getByLabelText('Phone number'))
    await user.click(screen.getByLabelText('Phone number'))
    await user.paste('1'.repeat(31))
    await user.click(saveButton())

    expect(await screen.findByText('First name must not exceed 100 characters')).toBeInTheDocument()
    expect(screen.getByText('Last name must not exceed 100 characters')).toBeInTheDocument()
    expect(screen.getByText('Phone number must not exceed 30 characters')).toBeInTheDocument()

    await user.clear(screen.getByLabelText('Phone number'))
    await user.type(screen.getByLabelText('Phone number'), '0912-abc')
    await user.click(saveButton())

    expect(
      await screen.findByText('Phone number may only contain digits, spaces, parentheses, hyphens and a leading +'),
    ).toBeInTheDocument()
    expect(requests).toBe(0)
  })

  it('disables the form while saving and sends only one request', async () => {
    let requests = 0
    mockApi({
      patch: async () => {
        requests += 1
        await delay('infinite')
        return HttpResponse.json(PROFILE)
      },
    })
    renderPage()
    const user = await waitForForm()

    await user.type(screen.getByLabelText('Last name'), 'x')
    await user.click(saveButton())
    await waitFor(() => expect(saveButton()).toBeDisabled())
    expect(cancelButton()).toBeDisabled()
    expect(screen.getByLabelText('First name')).toBeDisabled()
    await user.click(saveButton())

    expect(requests).toBe(1)
  })

  it('shows API validation errors under the matching fields', async () => {
    mockApi({
      patch: () =>
        HttpResponse.json(
          {
            status: 400,
            detail: 'One or more fields contain invalid values.',
            errorCode: 'VALIDATION_ERROR',
            fieldErrors: [{ field: 'phone', message: 'Phone number must not exceed 30 characters' }],
          },
          { status: 400, headers: problemHeaders },
        ),
    })
    renderPage()
    const user = await waitForForm()

    await user.type(screen.getByLabelText('Phone number'), '1')
    await user.click(saveButton())

    expect(await screen.findByText('Phone number must not exceed 30 characters')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(saveButton()).toBeEnabled()
  })

  it('shows an error and keeps the entered values when the API fails', async () => {
    mockApi({ patch: () => problem(500, 'INTERNAL_ERROR', 'An unexpected error occurred.') })
    renderPage()
    const user = await waitForForm()

    await user.clear(screen.getByLabelText('First name'))
    await user.type(screen.getByLabelText('First name'), 'Johnny')
    await user.click(saveButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('An unexpected error occurred.')
    expect(screen.getByLabelText('First name')).toHaveValue('Johnny')
    expect(saveButton()).toBeEnabled()
  })

  it('shows a generic error when the network fails', async () => {
    mockApi({ patch: () => HttpResponse.error() })
    renderPage()
    const user = await waitForForm()

    await user.type(screen.getByLabelText('Last name'), 'x')
    await user.click(saveButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong. Please try again.')
  })

  it('shows an error with a retry when the profile fails to load', async () => {
    let attempts = 0
    mockApi({
      get: () => {
        attempts += 1
        return attempts === 1 ? problem(500, 'INTERNAL_ERROR', 'An unexpected error occurred.') : HttpResponse.json(PROFILE)
      },
    })
    renderPage()

    expect(await screen.findByText("We couldn't load your profile.")).toBeInTheDocument()
    await userEvent.setup().click(screen.getByRole('button', { name: 'Retry' }))

    await waitForForm()
    expect(attempts).toBe(2)
  })

  it('reloads the saved profile after a refresh', async () => {
    let stored = PROFILE
    mockApi({
      get: () => HttpResponse.json(stored),
      patch: async ({ request }) => {
        stored = { ...stored, ...((await request.json()) as Partial<ClientProfile>) }
        return HttpResponse.json(stored)
      },
    })
    const { unmount } = renderPage()
    const user = await waitForForm()

    await user.clear(screen.getByLabelText('Last name'))
    await user.type(screen.getByLabelText('Last name'), 'Smith')
    await user.click(saveButton())
    await screen.findByText('Profile updated.')
    unmount()

    renderPage()
    expect(await screen.findByLabelText('Last name')).toHaveValue('Smith')
  })

  it('redirects to the login page when the session has expired', async () => {
    mockApi({ get: () => problem(401, 'UNAUTHORIZED', 'Authentication required.') })
    renderPage()

    expect(await screen.findByRole('heading', { name: /log in|sign in/i })).toBeInTheDocument()
  })
})
