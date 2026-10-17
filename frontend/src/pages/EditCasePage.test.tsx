import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse, delay } from 'msw'
import { Route, Routes, useParams, useRoutes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { routes } from '../routes/router'
import { server } from '../test/mocks/server'
import { renderWithProviders } from '../test/render'
import { editCasePath, paths } from '../routes/paths'
import EditCasePage from './EditCasePage'

const USER = { id: 'u1', email: 'john@example.com', firstName: 'John', lastName: 'Doe' }
const CSRF_TOKEN = 'csrf-token-123'
const CASE_ID = '0192f0a4-1111-7000-8000-000000000001'

const CATEGORIES = [
  { id: 'c-family', name: 'Family Law', description: null },
  { id: 'c-criminal', name: 'Criminal Law', description: null },
]

const DRAFT = {
  id: CASE_ID,
  title: 'Custody dispute',
  categoryId: 'c-family',
  description: 'Details of the dispute.',
  location: 'Manila',
  status: 'DRAFT',
  createdAt: '2026-10-17T10:00:00Z',
  updatedAt: '2026-10-17T10:00:00Z',
}

const problemHeaders = { 'Content-Type': 'application/problem+json' }

const problem = (status: number, errorCode: string, detail: string) =>
  HttpResponse.json({ status, errorCode, detail }, { status, headers: problemHeaders })

type Resolver = Parameters<typeof http.get>[1]

function mockApi({ get = () => HttpResponse.json(DRAFT), patch }: { get?: Resolver; patch?: Resolver } = {}) {
  server.use(
    http.get('*/api/v1/legal-categories', () => HttpResponse.json(CATEGORIES)),
    http.get('*/api/v1/auth/csrf', () => HttpResponse.json({ token: CSRF_TOKEN, headerName: 'X-XSRF-TOKEN' })),
    http.get(`*/api/v1/cases/${CASE_ID}`, get),
  )
  if (patch) {
    server.use(http.patch(`*/api/v1/cases/${CASE_ID}`, patch))
  }
}

function CaseDetailsStub() {
  const { id } = useParams()
  return <h1>Case {id}</h1>
}

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path={paths.editCase} element={<EditCasePage />} />
      <Route path={paths.caseDetails} element={<CaseDetailsStub />} />
    </Routes>,
    { initialEntries: [editCasePath(CASE_ID)], user: USER },
  )
}

const saveButton = () => screen.getByRole('button', { name: /save changes/i })
const categorySelect = () => screen.getByRole('combobox', { name: 'Legal category' })
const selectedCategory = () => within(categorySelect().closest('.ant-select') as HTMLElement)

// Waits for the case and its categories to load into the form.
async function waitForForm() {
  expect(await screen.findByLabelText('Title')).toHaveValue('Custody dispute')
  await waitFor(() => expect(categorySelect()).toBeEnabled())
  return userEvent.setup()
}

describe('EditCasePage', () => {
  it('shows a loading indicator while the case loads', async () => {
    mockApi({
      get: async () => {
        await delay('infinite')
        return HttpResponse.json(DRAFT)
      },
    })
    renderPage()

    expect(await screen.findByText('Loading case…')).toBeInTheDocument()
    expect(screen.queryByLabelText('Title')).not.toBeInTheDocument()
  })

  it('populates the form with the case details and preselects the category', async () => {
    mockApi()
    renderPage()

    await waitForForm()
    expect(screen.getByRole('heading', { name: 'Edit Draft Case' })).toBeInTheDocument()
    expect(screen.getByLabelText('Description')).toHaveValue('Details of the dispute.')
    expect(screen.getByLabelText('Location')).toHaveValue('Manila')
    expect(selectedCategory().getByText('Family Law')).toBeInTheDocument()
  })

  it('sends only the modified fields with the CSRF token and opens the case details page', async () => {
    let received: { body: unknown; token: string | null } | undefined
    mockApi({
      patch: async ({ request }) => {
        received = { body: await request.json(), token: request.headers.get('X-XSRF-TOKEN') }
        return HttpResponse.json({ ...DRAFT, title: 'Updated title', categoryId: 'c-criminal' })
      },
    })
    renderPage()
    const user = await waitForForm()

    await user.clear(screen.getByLabelText('Title'))
    await user.type(screen.getByLabelText('Title'), '  Updated title  ')
    await user.click(categorySelect())
    await user.click(await screen.findByTitle('Criminal Law'))
    await user.click(saveButton())

    expect(await screen.findByRole('heading', { name: `Case ${CASE_ID}` })).toBeInTheDocument()
    expect(await screen.findByText('Draft updated.')).toBeInTheDocument()
    expect(received).toEqual({ token: CSRF_TOKEN, body: { title: 'Updated title', categoryId: 'c-criminal' } })
  })

  it('sends null for cleared optional fields', async () => {
    let body: unknown
    mockApi({
      patch: async ({ request }) => {
        body = await request.json()
        return HttpResponse.json({ ...DRAFT, description: null, location: null })
      },
    })
    renderPage()
    const user = await waitForForm()

    await user.clear(screen.getByLabelText('Description'))
    await user.clear(screen.getByLabelText('Location'))
    await user.type(screen.getByLabelText('Location'), '   ')
    await user.click(saveButton())

    await screen.findByRole('heading', { name: `Case ${CASE_ID}` })
    expect(body).toEqual({ description: null, location: null })
  })

  it('skips the request when nothing changed', async () => {
    let requests = 0
    mockApi({
      patch: () => {
        requests += 1
        return HttpResponse.json(DRAFT)
      },
    })
    renderPage()
    const user = await waitForForm()

    await user.click(saveButton())

    expect(await screen.findByRole('heading', { name: `Case ${CASE_ID}` })).toBeInTheDocument()
    expect(await screen.findByText('No changes to save.')).toBeInTheDocument()
    expect(requests).toBe(0)
  })

  it('validates required fields and maximum lengths', async () => {
    mockApi()
    renderPage()
    const user = await waitForForm()

    await user.clear(screen.getByLabelText('Title'))
    await user.type(screen.getByLabelText('Title'), '   ')
    await user.click(saveButton())
    expect(await screen.findByText('Title is required')).toBeInTheDocument()

    await user.clear(screen.getByLabelText('Title'))
    await user.paste('a'.repeat(201))
    await user.clear(screen.getByLabelText('Location'))
    await user.paste('b'.repeat(256))
    await user.click(saveButton())

    expect(await screen.findByText('Title must not exceed 200 characters')).toBeInTheDocument()
    expect(screen.getByText('Location must not exceed 255 characters')).toBeInTheDocument()
  })

  it('disables the save button while saving and sends only one request', async () => {
    let requests = 0
    mockApi({
      patch: async () => {
        requests += 1
        await delay('infinite')
        return HttpResponse.json(DRAFT)
      },
    })
    renderPage()
    const user = await waitForForm()

    await user.type(screen.getByLabelText('Title'), '!')
    await user.click(saveButton())
    await waitFor(() => expect(saveButton()).toBeDisabled())
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
            fieldErrors: [{ field: 'categoryId', message: 'Category is not available' }],
          },
          { status: 400, headers: problemHeaders },
        ),
    })
    renderPage()
    const user = await waitForForm()

    await user.click(categorySelect())
    await user.click(await screen.findByTitle('Criminal Law'))
    await user.click(saveButton())

    expect(await screen.findByText('Category is not available')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(saveButton()).toBeEnabled()
  })

  it('shows an error and keeps the entered values when the API fails', async () => {
    mockApi({ patch: () => problem(500, 'INTERNAL_ERROR', 'An unexpected error occurred.') })
    renderPage()
    const user = await waitForForm()

    await user.clear(screen.getByLabelText('Title'))
    await user.type(screen.getByLabelText('Title'), 'Edited title')
    await user.click(saveButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('An unexpected error occurred.')
    expect(screen.getByLabelText('Title')).toHaveValue('Edited title')
    expect(screen.getByLabelText('Description')).toHaveValue('Details of the dispute.')
    expect(screen.getByLabelText('Location')).toHaveValue('Manila')
    expect(selectedCategory().getByText('Family Law')).toBeInTheDocument()
    expect(saveButton()).toBeEnabled()
  })

  it('shows the API message when the case is no longer a draft on save', async () => {
    mockApi({ patch: () => problem(409, 'INVALID_CASE_STATE', 'Only draft cases can be edited.') })
    renderPage()
    const user = await waitForForm()

    await user.type(screen.getByLabelText('Title'), '!')
    await user.click(saveButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('Only draft cases can be edited.')
    expect(screen.getByLabelText('Title')).toHaveValue('Custody dispute!')
  })

  it('shows an error with a retry when the case fails to load', async () => {
    let attempts = 0
    mockApi({
      get: () => {
        attempts += 1
        return attempts === 1 ? HttpResponse.error() : HttpResponse.json(DRAFT)
      },
    })
    renderPage()
    const user = userEvent.setup()

    expect(await screen.findByText("We couldn't load this case.")).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Retry' }))

    await waitForForm()
    expect(attempts).toBe(2)
  })

  it.each([
    ['SUBMITTED', 'submitted'],
    ['CLOSED', 'closed'],
  ])('does not allow editing a %s case', async (status, label) => {
    mockApi({ get: () => HttpResponse.json({ ...DRAFT, status }) })
    renderPage()
    const user = userEvent.setup()

    expect(await screen.findByText("This case can't be edited")).toBeInTheDocument()
    expect(screen.getByText(`Only drafts can be edited. This case is ${label}.`)).toBeInTheDocument()
    expect(screen.queryByLabelText('Title')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'View case' }))
    expect(await screen.findByRole('heading', { name: `Case ${CASE_ID}` })).toBeInTheDocument()
  })

  it('shows a not-found state for missing or inaccessible cases', async () => {
    mockApi({ get: () => problem(404, 'CASE_NOT_FOUND', 'Case not found.') })
    renderPage()

    expect(await screen.findByText('Case not found')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Back to My Cases' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Title')).not.toBeInTheDocument()
  })

  it('redirects to the login page when the session has expired', async () => {
    mockApi({ get: () => problem(401, 'UNAUTHORIZED', 'Authentication required.') })
    function AppRoutes() {
      return useRoutes(routes)
    }
    renderWithProviders(<AppRoutes />, { initialEntries: [editCasePath(CASE_ID)], user: USER })

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
  })
})
