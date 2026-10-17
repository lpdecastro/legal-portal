import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse, delay } from 'msw'
import { Route, Routes, useParams } from 'react-router'
import { describe, expect, it } from 'vitest'
import { server } from '../test/mocks/server'
import { renderWithProviders } from '../test/render'
import { paths } from '../routes/paths'
import CreateCasePage from './CreateCasePage'

const USER = { id: 'u1', email: 'john@example.com', firstName: 'John', lastName: 'Doe' }
const CSRF_TOKEN = 'csrf-token-123'

const CATEGORIES = [
  { id: 'c-family', name: 'Family Law', description: null },
  { id: 'c-criminal', name: 'Criminal Law', description: null },
]

const CREATED_CASE = {
  id: 'case-1',
  title: 'Custody dispute',
  categoryId: 'c-family',
  description: 'Details of the dispute.',
  location: 'Manila',
  status: 'DRAFT',
  createdAt: '2026-10-17T10:00:00Z',
  updatedAt: '2026-10-17T10:00:00Z',
}

const problemHeaders = { 'Content-Type': 'application/problem+json' }

function mockApi(createResolver?: Parameters<typeof http.post>[1]) {
  server.use(
    http.get('*/api/v1/legal-categories', () => HttpResponse.json(CATEGORIES)),
    http.get('*/api/v1/auth/csrf', () => HttpResponse.json({ token: CSRF_TOKEN, headerName: 'X-XSRF-TOKEN' })),
  )
  if (createResolver) {
    server.use(http.post('*/api/v1/cases', createResolver))
  }
}

function CaseDetailsStub() {
  const { id } = useParams()
  return <h1>Case {id}</h1>
}

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path={paths.createCase} element={<CreateCasePage />} />
      <Route path={paths.caseDetails} element={<CaseDetailsStub />} />
    </Routes>,
    { initialEntries: [paths.createCase], user: USER },
  )
}

const saveButton = () => screen.getByRole('button', { name: /save draft/i })
const categorySelect = () => screen.getByRole('combobox', { name: 'Legal category' })

async function fillForm({ title = 'Custody dispute', description = 'Details of the dispute.', location = 'Manila' } = {}) {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('Title'), title)
  await waitFor(() => expect(categorySelect()).toBeEnabled())
  await user.click(categorySelect())
  await user.click(await screen.findByTitle('Family Law'))
  if (description) await user.type(screen.getByLabelText('Description'), description)
  if (location) await user.type(screen.getByLabelText('Location'), location)
  return user
}

describe('CreateCasePage', () => {
  it('renders the form fields and loads the legal categories', async () => {
    mockApi()
    renderPage()
    const user = userEvent.setup()

    expect(screen.getByRole('heading', { name: 'Create Draft Case' })).toBeInTheDocument()
    expect(screen.getByLabelText('Title')).toBeInTheDocument()
    expect(screen.getByLabelText('Description')).toBeInTheDocument()
    expect(screen.getByLabelText('Location')).toBeInTheDocument()
    expect(saveButton()).toBeInTheDocument()

    await waitFor(() => expect(categorySelect()).toBeEnabled())
    await user.click(categorySelect())
    expect(await screen.findByTitle('Criminal Law')).toBeInTheDocument()
    expect(screen.getByTitle('Family Law')).toBeInTheDocument()
  })

  it('shows required-field messages when saved empty', async () => {
    mockApi()
    renderPage()
    const user = userEvent.setup()

    await user.click(saveButton())

    expect(await screen.findByText('Title is required')).toBeInTheDocument()
    expect(screen.getByText('Legal category is required')).toBeInTheDocument()
  })

  it('rejects a blank title', async () => {
    mockApi()
    renderPage()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText('Title'), '   ')
    await user.click(saveButton())

    expect(await screen.findByText('Title is required')).toBeInTheDocument()
  })

  it('validates the maximum title and location lengths', async () => {
    mockApi()
    renderPage()
    const user = userEvent.setup()

    await user.click(screen.getByLabelText('Title'))
    await user.paste('a'.repeat(201))
    await user.click(screen.getByLabelText('Location'))
    await user.paste('b'.repeat(256))
    await user.click(saveButton())

    expect(await screen.findByText('Title must not exceed 200 characters')).toBeInTheDocument()
    expect(screen.getByText('Location must not exceed 255 characters')).toBeInTheDocument()
  })

  it('saves the draft with the CSRF token and opens the case details page', async () => {
    let received: { body: unknown; token: string | null } | undefined
    mockApi(async ({ request }) => {
      received = { body: await request.json(), token: request.headers.get('X-XSRF-TOKEN') }
      return HttpResponse.json(CREATED_CASE, { status: 201 })
    })
    renderPage()
    const user = await fillForm({ title: '  Custody dispute  ', location: ' Manila ' })

    await user.click(saveButton())

    expect(await screen.findByRole('heading', { name: 'Case case-1' })).toBeInTheDocument()
    expect(await screen.findByText('Draft saved.')).toBeInTheDocument()
    expect(received).toEqual({
      token: CSRF_TOKEN,
      body: {
        title: 'Custody dispute',
        categoryId: 'c-family',
        description: 'Details of the dispute.',
        location: 'Manila',
      },
    })
  })

  it('omits empty optional fields', async () => {
    let body: unknown
    mockApi(async ({ request }) => {
      body = await request.json()
      return HttpResponse.json({ ...CREATED_CASE, description: null, location: null }, { status: 201 })
    })
    renderPage()
    const user = await fillForm({ description: '', location: '' })

    await user.click(saveButton())

    await screen.findByRole('heading', { name: 'Case case-1' })
    expect(body).toEqual({ title: 'Custody dispute', categoryId: 'c-family' })
  })

  it('disables the save button while saving and sends only one request', async () => {
    let requests = 0
    mockApi(async () => {
      requests += 1
      await delay('infinite')
      return HttpResponse.json(CREATED_CASE, { status: 201 })
    })
    renderPage()
    const user = await fillForm()

    await user.click(saveButton())
    await waitFor(() => expect(saveButton()).toBeDisabled())
    await user.click(saveButton())

    expect(requests).toBe(1)
  })

  it('shows API validation errors under the matching fields', async () => {
    mockApi(() =>
      HttpResponse.json(
        {
          status: 400,
          title: 'Validation Failed',
          detail: 'One or more fields contain invalid values.',
          errorCode: 'VALIDATION_ERROR',
          fieldErrors: [{ field: 'categoryId', message: 'Category is not available' }],
        },
        { status: 400, headers: problemHeaders },
      ),
    )
    renderPage()
    const user = await fillForm()

    await user.click(saveButton())

    expect(await screen.findByText('Category is not available')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(saveButton()).toBeEnabled()
  })

  it('shows an error and keeps the entered values when the API fails', async () => {
    mockApi(() =>
      HttpResponse.json(
        { status: 500, title: 'Internal Server Error', detail: 'An unexpected error occurred.', errorCode: 'INTERNAL_ERROR' },
        { status: 500, headers: problemHeaders },
      ),
    )
    renderPage()
    const user = await fillForm()

    await user.click(saveButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('An unexpected error occurred.')
    expect(screen.getByLabelText('Title')).toHaveValue('Custody dispute')
    expect(screen.getByLabelText('Description')).toHaveValue('Details of the dispute.')
    expect(screen.getByLabelText('Location')).toHaveValue('Manila')
    const field = categorySelect().closest('.ant-select') as HTMLElement
    expect(within(field).getByText('Family Law')).toBeInTheDocument()
    expect(saveButton()).toBeEnabled()
  })

  it('shows a generic error when the server cannot be reached', async () => {
    mockApi(() => HttpResponse.error())
    renderPage()
    const user = await fillForm()

    await user.click(saveButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong. Please try again.')
    expect(screen.getByLabelText('Title')).toHaveValue('Custody dispute')
  })
})
