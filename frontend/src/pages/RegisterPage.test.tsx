import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse, delay } from 'msw'
import { Route, Routes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { server } from '../test/mocks/server'
import { renderWithProviders } from '../test/render'
import { paths } from '../routes/paths'
import RegisterPage from './RegisterPage'

const CSRF_TOKEN = 'csrf-token-123'
const VALID_PASSWORD = 'correct-horse-battery'

const problemHeaders = { 'Content-Type': 'application/problem+json' }

function mockCsrf() {
  server.use(
    http.get('*/api/v1/auth/csrf', () =>
      HttpResponse.json({ token: CSRF_TOKEN, headerName: 'X-XSRF-TOKEN' }),
    ),
  )
}

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path={paths.register} element={<RegisterPage />} />
      <Route path={paths.login} element={<h1>Login page</h1>} />
    </Routes>,
    { initialEntries: [paths.register] },
  )
}

async function fillForm(overrides: Partial<Record<string, string>> = {}) {
  const user = userEvent.setup()
  const values = {
    'First name': 'John',
    'Last name': 'Doe',
    Email: 'john@example.com',
    Password: VALID_PASSWORD,
    'Confirm password': VALID_PASSWORD,
    ...overrides,
  }
  for (const [label, value] of Object.entries(values)) {
    if (value) {
      await user.type(screen.getByLabelText(label), value)
    }
  }
  return user
}

const submitButton = () => screen.getByRole('button', { name: /create account/i })

describe('RegisterPage', () => {
  it('shows required-field messages when submitted empty', async () => {
    renderPage()
    const user = userEvent.setup()

    await user.click(submitButton())

    expect(await screen.findByText('First name is required')).toBeInTheDocument()
    expect(screen.getByText('Last name is required')).toBeInTheDocument()
    expect(screen.getByText('Email is required')).toBeInTheDocument()
    expect(screen.getByText('Password is required')).toBeInTheDocument()
    expect(screen.getByText('Please confirm your password')).toBeInTheDocument()
  })

  it('validates email format, password length, and matching passwords', async () => {
    renderPage()
    const user = await fillForm({
      Email: 'not-an-email',
      Password: 'short',
      'Confirm password': 'different',
    })

    await user.click(submitButton())

    expect(await screen.findByText('Invalid email address')).toBeInTheDocument()
    expect(screen.getByText('Password must contain at least 12 characters')).toBeInTheDocument()
    expect(screen.getByText('Passwords do not match')).toBeInTheDocument()
  })

  it('registers with the CSRF token and redirects to login', async () => {
    mockCsrf()
    let received: { body: unknown; token: string | null } | undefined
    server.use(
      http.post('*/api/v1/auth/register', async ({ request }) => {
        received = { body: await request.json(), token: request.headers.get('X-XSRF-TOKEN') }
        return HttpResponse.json(
          { id: 'u1', email: 'john@example.com', firstName: 'John', lastName: 'Doe' },
          { status: 201 },
        )
      }),
    )
    renderPage()
    const user = await fillForm()

    await user.click(submitButton())

    expect(await screen.findByRole('heading', { name: 'Login page' })).toBeInTheDocument()
    expect(received).toEqual({
      token: CSRF_TOKEN,
      body: {
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        password: VALID_PASSWORD,
      },
    })
  })

  it('disables the submit button while submitting', async () => {
    mockCsrf()
    server.use(
      http.post('*/api/v1/auth/register', async () => {
        await delay('infinite')
        return HttpResponse.json({}, { status: 201 })
      }),
    )
    renderPage()
    const user = await fillForm()

    await user.click(submitButton())

    await waitFor(() => expect(submitButton()).toBeDisabled())
  })

  it('shows an error on the email field for a duplicate email', async () => {
    mockCsrf()
    server.use(
      http.post('*/api/v1/auth/register', () =>
        HttpResponse.json(
          {
            status: 409,
            title: 'Conflict',
            detail: 'Email address is already registered.',
            errorCode: 'EMAIL_ALREADY_EXISTS',
          },
          { status: 409, headers: problemHeaders },
        ),
      ),
    )
    renderPage()
    const user = await fillForm()

    await user.click(submitButton())

    expect(await screen.findByText('Email address is already registered.')).toBeInTheDocument()
    expect(submitButton()).toBeEnabled()
    expect(screen.queryByRole('heading', { name: 'Login page' })).not.toBeInTheDocument()
  })

  it('maps API field errors onto the form', async () => {
    mockCsrf()
    server.use(
      http.post('*/api/v1/auth/register', () =>
        HttpResponse.json(
          {
            status: 400,
            title: 'Validation Failed',
            detail: 'One or more fields contain invalid values.',
            errorCode: 'VALIDATION_ERROR',
            fieldErrors: [{ field: 'password', message: 'Password must not exceed 72 bytes' }],
          },
          { status: 400, headers: problemHeaders },
        ),
      ),
    )
    renderPage()
    const user = await fillForm()

    await user.click(submitButton())

    expect(await screen.findByText('Password must not exceed 72 bytes')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('shows an alert for other API errors', async () => {
    mockCsrf()
    server.use(
      http.post('*/api/v1/auth/register', () =>
        HttpResponse.json(
          { status: 403, title: 'Forbidden', detail: 'Invalid or missing CSRF token.', errorCode: 'FORBIDDEN' },
          { status: 403, headers: problemHeaders },
        ),
      ),
    )
    renderPage()
    const user = await fillForm()

    await user.click(submitButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid or missing CSRF token.')
  })

  it('shows a generic alert when the server cannot be reached', async () => {
    server.use(http.get('*/api/v1/auth/csrf', () => HttpResponse.error()))
    renderPage()
    const user = await fillForm()

    await user.click(submitButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong. Please try again.')
  })
})
