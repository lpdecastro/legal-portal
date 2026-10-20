import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse, delay } from 'msw'
import { useRoutes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { routes } from '../routes/router'
import { paths } from '../routes/paths'
import { server } from '../test/mocks/server'
import { renderWithProviders } from '../test/render'

const USER = { id: 'u1', email: 'john@example.com', firstName: 'John', lastName: 'Doe' }
const CSRF_TOKEN = 'csrf-token-123'

const CURRENT = 'old password 123'
const NEW = 'new password 456'

const problemHeaders = { 'Content-Type': 'application/problem+json' }

const problem = (status: number, errorCode: string, detail: string) =>
  HttpResponse.json({ status, errorCode, detail }, { status, headers: problemHeaders })

type Resolver = Parameters<typeof http.patch>[1]

function mockApi(patch: Resolver = () => new HttpResponse(null, { status: 204 })) {
  server.use(
    http.get('*/api/v1/auth/csrf', () => HttpResponse.json({ token: CSRF_TOKEN, headerName: 'X-XSRF-TOKEN' })),
    http.patch('*/api/v1/clients/me/password', patch),
  )
}

function AppRoutes() {
  return useRoutes(routes)
}

function renderPage() {
  renderWithProviders(<AppRoutes />, { initialEntries: [paths.settings], user: USER })
  return userEvent.setup()
}

const currentField = () => screen.getByLabelText('Current password')
const newField = () => screen.getByLabelText('New password')
const confirmField = () => screen.getByLabelText('Confirm new password')
const submitButton = () => screen.getByRole('button', { name: /change password/i })

async function fillForm(user: ReturnType<typeof userEvent.setup>, current = CURRENT, next = NEW, confirm = next) {
  await user.type(currentField(), current)
  await user.type(newField(), next)
  await user.type(confirmField(), confirm)
}

describe('SettingsPage change password', () => {
  it('shows the three password fields, masked', () => {
    mockApi()
    renderPage()

    expect(screen.getByRole('heading', { name: 'Settings' })).toBeInTheDocument()
    for (const field of [currentField(), newField(), confirmField()]) {
      expect(field).toHaveAttribute('type', 'password')
    }
  })

  it('shows and hides each password', async () => {
    mockApi()
    const user = renderPage()

    const toggles = screen.getAllByRole('img', { name: /eye/ })
    expect(toggles).toHaveLength(3)
    await user.click(toggles[1])
    expect(newField()).toHaveAttribute('type', 'text')
    expect(currentField()).toHaveAttribute('type', 'password')
    await user.click(screen.getAllByRole('img', { name: /eye/ })[1])
    expect(newField()).toHaveAttribute('type', 'password')
  })

  it('sends the passwords as entered with the CSRF token, then confirms and clears the fields', async () => {
    let body: unknown
    let csrf: string | null = null
    mockApi(async ({ request }) => {
      body = await request.json()
      csrf = request.headers.get('X-XSRF-TOKEN')
      return new HttpResponse(null, { status: 204 })
    })
    const user = renderPage()

    await fillForm(user, ` ${CURRENT} `, ` ${NEW} `)
    await user.click(submitButton())

    expect(await screen.findByText('Password changed.')).toBeInTheDocument()
    expect(body).toEqual({ currentPassword: ` ${CURRENT} `, newPassword: ` ${NEW} `, confirmPassword: ` ${NEW} ` })
    expect(csrf).toBe(CSRF_TOKEN)
    await waitFor(() => expect(currentField()).toHaveValue(''))
    expect(newField()).toHaveValue('')
    expect(confirmField()).toHaveValue('')
  })

  it('never stores the passwords in browser storage', async () => {
    mockApi()
    const user = renderPage()

    await fillForm(user)
    await user.click(submitButton())
    await screen.findByText('Password changed.')

    const stored = [localStorage, sessionStorage].flatMap((storage) =>
      Array.from({ length: storage.length }, (_, i) => storage.getItem(storage.key(i) ?? '') ?? ''),
    )
    expect(stored.some((value) => value.includes(CURRENT) || value.includes(NEW))).toBe(false)
  })

  it('requires every field', async () => {
    let requests = 0
    mockApi(() => {
      requests += 1
      return new HttpResponse(null, { status: 204 })
    })
    const user = renderPage()

    await user.click(submitButton())

    expect(await screen.findByText('Current password is required')).toBeInTheDocument()
    expect(screen.getByText('New password is required')).toBeInTheDocument()
    expect(screen.getByText('Please confirm your new password')).toBeInTheDocument()
    expect(requests).toBe(0)
  })

  it('validates the new password length and confirmation', async () => {
    let requests = 0
    mockApi(() => {
      requests += 1
      return new HttpResponse(null, { status: 204 })
    })
    const user = renderPage()

    await fillForm(user, CURRENT, 'short', 'different')
    await user.click(submitButton())

    expect(await screen.findByText('Password must contain at least 12 characters')).toBeInTheDocument()
    expect(screen.getByText('Passwords do not match')).toBeInTheDocument()

    await user.clear(newField())
    await user.type(newField(), 'é'.repeat(37))
    expect(await screen.findByText('Password is too long')).toBeInTheDocument()
    expect(requests).toBe(0)
  })

  it('disables the form while changing and sends only one request', async () => {
    let requests = 0
    mockApi(async () => {
      requests += 1
      await delay('infinite')
      return new HttpResponse(null, { status: 204 })
    })
    const user = renderPage()

    await fillForm(user)
    await user.click(submitButton())
    await waitFor(() => expect(submitButton()).toBeDisabled())
    expect(currentField()).toBeDisabled()
    await user.click(submitButton())

    expect(requests).toBe(1)
  })

  it('shows an incorrect current password under its field and keeps the values', async () => {
    mockApi(() => problem(403, 'INCORRECT_PASSWORD', 'Current password is incorrect.'))
    const user = renderPage()

    await fillForm(user)
    await user.click(submitButton())

    expect(await screen.findByText('Current password is incorrect.')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(currentField()).toHaveValue(CURRENT)
    expect(newField()).toHaveValue(NEW)
    expect(submitButton()).toBeEnabled()
  })

  it('shows API validation errors under the matching fields', async () => {
    mockApi(() =>
      HttpResponse.json(
        {
          status: 400,
          detail: 'One or more fields contain invalid values.',
          errorCode: 'VALIDATION_ERROR',
          fieldErrors: [{ field: 'newPassword', message: 'New password must differ from the current password' }],
        },
        { status: 400, headers: problemHeaders },
      ),
    )
    const user = renderPage()

    await fillForm(user)
    await user.click(submitButton())

    expect(await screen.findByText('New password must differ from the current password')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('shows an error and keeps the entered values when the API fails', async () => {
    mockApi(() => problem(500, 'INTERNAL_ERROR', 'An unexpected error occurred.'))
    const user = renderPage()

    await fillForm(user)
    await user.click(submitButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('An unexpected error occurred.')
    expect(newField()).toHaveValue(NEW)
    expect(submitButton()).toBeEnabled()
  })

  it('shows a generic error when the network fails', async () => {
    mockApi(() => HttpResponse.error())
    const user = renderPage()

    await fillForm(user)
    await user.click(submitButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong. Please try again.')
  })

  it('redirects to the login page when the session has expired', async () => {
    mockApi(() => problem(401, 'UNAUTHORIZED', 'Authentication required.'))
    const user = renderPage()

    await fillForm(user)
    await user.click(submitButton())

    expect(await screen.findByRole('heading', { name: /log in|sign in/i })).toBeInTheDocument()
  })
})
