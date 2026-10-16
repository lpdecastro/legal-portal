import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse, delay } from 'msw'
import { Route, Routes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { useAuth } from '../hooks/useAuth'
import { server } from '../test/mocks/server'
import { renderWithProviders } from '../test/render'
import { paths } from '../routes/paths'
import LoginPage from './LoginPage'

const CSRF_TOKEN = 'csrf-token-123'
const PASSWORD = 'correct-horse-battery'
const USER = { id: 'u1', email: 'john@example.com', firstName: 'John', lastName: 'Doe' }

const problemHeaders = { 'Content-Type': 'application/problem+json' }

function mockCsrf() {
  server.use(
    http.get('*/api/v1/auth/csrf', () =>
      HttpResponse.json({ token: CSRF_TOKEN, headerName: 'X-XSRF-TOKEN' }),
    ),
  )
}

function mockLoginProblem(status: number, problem: Record<string, unknown>) {
  server.use(
    http.post('*/api/v1/auth/login', () =>
      HttpResponse.json({ status, ...problem }, { status, headers: problemHeaders }),
    ),
  )
}

// Stands in for the dashboard and shows who auth state says is signed in.
function DashboardStub() {
  const { user } = useAuth()
  return <h1>Dashboard for {user?.firstName}</h1>
}

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path={paths.login} element={<LoginPage />} />
      <Route path={paths.dashboard} element={<DashboardStub />} />
    </Routes>,
    { initialEntries: [paths.login] },
  )
}

async function fillForm({ email = 'john@example.com', password = PASSWORD } = {}) {
  const user = userEvent.setup()
  if (email) await user.type(screen.getByLabelText('Email'), email)
  if (password) await user.type(screen.getByLabelText('Password'), password)
  return user
}

const submitButton = () => screen.getByRole('button', { name: /log in/i })

describe('LoginPage', () => {
  it('shows required-field messages when submitted empty', async () => {
    renderPage()
    const user = userEvent.setup()

    await user.click(submitButton())

    expect(await screen.findByText('Email is required')).toBeInTheDocument()
    expect(screen.getByText('Password is required')).toBeInTheDocument()
  })

  it('validates the email format', async () => {
    renderPage()
    const user = await fillForm({ email: 'not-an-email' })

    await user.click(submitButton())

    expect(await screen.findByText('Invalid email address')).toBeInTheDocument()
  })

  it('logs in with the CSRF token, stores the user, and redirects to the dashboard', async () => {
    mockCsrf()
    let received: { body: unknown; token: string | null } | undefined
    server.use(
      http.post('*/api/v1/auth/login', async ({ request }) => {
        received = { body: await request.json(), token: request.headers.get('X-XSRF-TOKEN') }
        return HttpResponse.json(USER)
      }),
    )
    renderPage()
    const user = await fillForm({ email: '  john@example.com  ' })

    await user.click(submitButton())

    expect(await screen.findByRole('heading', { name: 'Dashboard for John' })).toBeInTheDocument()
    expect(received).toEqual({
      token: CSRF_TOKEN,
      body: { email: 'john@example.com', password: PASSWORD },
    })
  })

  it('disables the submit button while submitting', async () => {
    mockCsrf()
    server.use(
      http.post('*/api/v1/auth/login', async () => {
        await delay('infinite')
        return HttpResponse.json(USER)
      }),
    )
    renderPage()
    const user = await fillForm()

    await user.click(submitButton())

    await waitFor(() => expect(submitButton()).toBeDisabled())
  })

  it('shows a generic error for invalid credentials', async () => {
    mockCsrf()
    mockLoginProblem(401, { title: 'Unauthorized', detail: 'Bad credentials', errorCode: 'UNAUTHORIZED' })
    renderPage()
    const user = await fillForm()

    await user.click(submitButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password.')
    expect(submitButton()).toBeEnabled()
    expect(screen.queryByRole('heading', { name: /dashboard/i })).not.toBeInTheDocument()
  })

  it('shows the API message when login is rate-limited', async () => {
    mockCsrf()
    mockLoginProblem(429, {
      title: 'Too Many Requests',
      detail: 'Too many failed login attempts. Please try again later.',
      errorCode: 'RATE_LIMIT_EXCEEDED',
    })
    renderPage()
    const user = await fillForm()

    await user.click(submitButton())

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Too many failed login attempts. Please try again later.',
    )
  })

  it('maps API field errors onto the form', async () => {
    mockCsrf()
    mockLoginProblem(400, {
      title: 'Validation Failed',
      detail: 'One or more fields contain invalid values.',
      errorCode: 'VALIDATION_ERROR',
      fieldErrors: [{ field: 'email', message: 'Email must not exceed 254 characters' }],
    })
    renderPage()
    const user = await fillForm()

    await user.click(submitButton())

    expect(await screen.findByText('Email must not exceed 254 characters')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('shows a generic alert when the server cannot be reached', async () => {
    server.use(http.get('*/api/v1/auth/csrf', () => HttpResponse.error()))
    renderPage()
    const user = await fillForm()

    await user.click(submitButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong. Please try again.')
  })
})
