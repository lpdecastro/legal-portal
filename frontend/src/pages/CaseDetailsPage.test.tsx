import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse, delay } from 'msw'
import { Route, Routes, useParams, useRoutes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { server } from '../test/mocks/server'
import { renderWithProviders } from '../test/render'
import { caseDetailsPath, paths } from '../routes/paths'
import { routes } from '../routes/router'
import CaseDetailsPage from './CaseDetailsPage'

const USER = { id: 'u1', email: 'john@example.com', firstName: 'John', lastName: 'Doe' }
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
  updatedAt: '2026-10-18T10:00:00Z',
}

const CSRF = { token: 'csrf-token', headerName: 'X-XSRF-TOKEN' }

const problemHeaders = { 'Content-Type': 'application/problem+json' }

const problem = (status: number, errorCode: string, detail: string) =>
  HttpResponse.json({ status, errorCode, detail }, { status, headers: problemHeaders })

type Resolver = Parameters<typeof http.get>[1]

function mockApi({ get = () => HttpResponse.json(DRAFT) }: { get?: Resolver } = {}) {
  server.use(
    http.get('*/api/v1/legal-categories', () => HttpResponse.json(CATEGORIES)),
    http.get(`*/api/v1/cases/${CASE_ID}`, get),
  )
}

function EditStub() {
  const { id } = useParams()
  return <h1>Edit {id}</h1>
}

function renderPage(id = CASE_ID) {
  return renderWithProviders(
    <Routes>
      <Route path={paths.caseDetails} element={<CaseDetailsPage />} />
      <Route path={paths.editCase} element={<EditStub />} />
      <Route path={paths.cases} element={<h1>My Cases</h1>} />
    </Routes>,
    { initialEntries: [caseDetailsPath(id)], user: USER },
  )
}

function mockDelete(resolver: Parameters<typeof http.delete>[1]) {
  server.use(
    http.get('*/api/v1/auth/csrf', () => HttpResponse.json(CSRF)),
    http.delete(`*/api/v1/cases/${CASE_ID}`, resolver),
  )
}

async function openDeleteDialog() {
  const user = userEvent.setup()
  await user.click(await screen.findByRole('button', { name: /delete/i }))
  const dialog = await screen.findByRole('dialog', { name: 'Delete this draft?' })
  return { user, dialog }
}

const confirmButton = (dialog: HTMLElement) => within(dialog).getByRole('button', { name: /delete/i })

const button = (name: string) => screen.queryByRole('button', { name: new RegExp(name, 'i') })

describe('CaseDetailsPage', () => {
  it('shows a loading indicator while the case loads', async () => {
    mockApi({
      get: async () => {
        await delay('infinite')
        return HttpResponse.json(DRAFT)
      },
    })
    renderPage()

    expect(await screen.findByText('Loading case…')).toBeInTheDocument()
  })

  it("shows the case's details", async () => {
    mockApi()
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Custody dispute' })).toBeInTheDocument()
    expect(screen.getByText('Draft')).toBeInTheDocument()
    expect(await screen.findByText('Family Law')).toBeInTheDocument()
    expect(screen.getByText('Details of the dispute.')).toBeInTheDocument()
    expect(screen.getByText('Manila')).toBeInTheDocument()
    expect(screen.getByText('Oct 17, 2026')).toBeInTheDocument()
    expect(screen.getByText('Oct 18, 2026')).toBeInTheDocument()
  })

  it('marks missing optional fields and unknown categories', async () => {
    mockApi({ get: () => HttpResponse.json({ ...DRAFT, categoryId: 'c-inactive', description: null, location: null }) })
    renderPage()

    expect(await screen.findByText('Unavailable')).toBeInTheDocument()
    expect(screen.getAllByText('Not provided')).toHaveLength(2)
  })

  it('offers Edit, Submit and Delete for a draft', async () => {
    mockApi()
    renderPage()

    await screen.findByRole('heading', { name: 'Custody dispute' })
    expect(button('edit')).toBeEnabled()
    expect(button('submit')).toBeInTheDocument()
    expect(button('delete')).toBeInTheDocument()
    expect(button('close')).not.toBeInTheDocument()
  })

  it('offers only Close for a submitted case', async () => {
    mockApi({ get: () => HttpResponse.json({ ...DRAFT, status: 'SUBMITTED' }) })
    renderPage()

    expect(await screen.findByText('Submitted')).toBeInTheDocument()
    expect(button('close')).toBeInTheDocument()
    expect(button('edit')).not.toBeInTheDocument()
    expect(button('submit')).not.toBeInTheDocument()
    expect(button('delete')).not.toBeInTheDocument()
  })

  it('offers no actions for a closed case', async () => {
    mockApi({ get: () => HttpResponse.json({ ...DRAFT, status: 'CLOSED' }) })
    renderPage()

    expect(await screen.findByText('Closed')).toBeInTheDocument()
    for (const name of ['edit', 'submit', 'delete', 'close']) {
      expect(button(name)).not.toBeInTheDocument()
    }
  })

  it('navigates to the edit page when Edit is clicked', async () => {
    mockApi()
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /edit/i }))

    expect(await screen.findByRole('heading', { name: `Edit ${CASE_ID}` })).toBeInTheDocument()
  })

  it('shows a not-found message for a missing or inaccessible case', async () => {
    mockApi({ get: () => problem(404, 'CASE_NOT_FOUND', 'Case not found.') })
    renderPage()

    expect(await screen.findByText('Case not found')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Custody dispute' })).not.toBeInTheDocument()
  })

  it('shows a not-found message for a malformed case ID', async () => {
    server.use(
      http.get('*/api/v1/cases/not-a-uuid', () =>
        problem(400, 'VALIDATION_ERROR', "Parameter 'id' has an invalid value."),
      ),
    )
    renderPage('not-a-uuid')

    expect(await screen.findByText('Case not found')).toBeInTheDocument()
  })

  it('shows an error with Retry when loading fails, and recovers on retry', async () => {
    let calls = 0
    mockApi({
      get: () => {
        calls += 1
        return calls === 1 ? problem(500, 'INTERNAL_ERROR', 'Unexpected error.') : HttpResponse.json(DRAFT)
      },
    })
    const user = userEvent.setup()
    renderPage()

    expect(await screen.findByText("We couldn't load this case.")).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /retry/i }))

    expect(await screen.findByRole('heading', { name: 'Custody dispute' })).toBeInTheDocument()
  })

  describe('delete', () => {
    it('asks for confirmation before deleting', async () => {
      mockApi()
      renderPage()

      const { dialog } = await openDeleteDialog()

      expect(within(dialog).getByRole('button', { name: /cancel/i })).toBeInTheDocument()
      expect(confirmButton(dialog)).toBeInTheDocument()
    })

    it('does not delete when the client cancels', async () => {
      let deleted = false
      mockApi()
      mockDelete(() => {
        deleted = true
        return new HttpResponse(null, { status: 204 })
      })
      renderPage()

      const { user, dialog } = await openDeleteDialog()
      await user.click(within(dialog).getByRole('button', { name: /cancel/i }))

      await waitFor(() => expect(dialog).not.toBeVisible())
      expect(deleted).toBe(false)
      expect(screen.getByRole('heading', { name: 'Custody dispute' })).toBeInTheDocument()
    })

    it('deletes the draft with the CSRF token, confirms and goes to My Cases', async () => {
      let csrfHeader: string | null = null
      mockApi()
      mockDelete(({ request }) => {
        csrfHeader = request.headers.get(CSRF.headerName)
        return new HttpResponse(null, { status: 204 })
      })
      renderPage()

      const { user, dialog } = await openDeleteDialog()
      await user.click(confirmButton(dialog))

      expect(await screen.findByRole('heading', { name: 'My Cases' })).toBeInTheDocument()
      expect(await screen.findByText('Draft deleted.')).toBeInTheDocument()
      expect(csrfHeader).toBe(CSRF.token)
    })

    it('shows a loading indicator and sends only one request while deleting', async () => {
      let calls = 0
      mockApi()
      mockDelete(async () => {
        calls += 1
        await delay('infinite')
        return new HttpResponse(null, { status: 204 })
      })
      renderPage()

      const { user, dialog } = await openDeleteDialog()
      const confirm = confirmButton(dialog)
      await user.click(confirm)

      await waitFor(() => expect(confirm).toHaveClass('ant-btn-loading'))
      await user.click(confirm)
      expect(within(dialog).getByRole('button', { name: /cancel/i })).toBeDisabled()
      expect(calls).toBe(1)
    })

    it.each([
      [500, 'INTERNAL_ERROR', "We couldn't delete this case. Please try again."],
      [404, 'CASE_NOT_FOUND', 'This case no longer exists or you no longer have access to it.'],
      [409, 'INVALID_CASE_STATE', 'Only draft cases can be deleted.'],
    ])('shows an error and stays on the page when the API answers %i', async (status, errorCode, text) => {
      mockApi()
      mockDelete(() => problem(status, errorCode, 'Rejected.'))
      renderPage()

      const { user, dialog } = await openDeleteDialog()
      await user.click(confirmButton(dialog))

      expect(await screen.findByText(text)).toBeInTheDocument()
      expect(screen.getByRole('heading', { name: 'Custody dispute' })).toBeInTheDocument()
      expect(screen.queryByRole('heading', { name: 'My Cases' })).not.toBeInTheDocument()
    })

    it('sends the client to the login page when the session has expired', async () => {
      function AppRoutes() {
        return useRoutes(routes)
      }
      mockApi()
      mockDelete(() => problem(401, 'UNAUTHORIZED', 'Authentication required.'))
      renderWithProviders(<AppRoutes />, { initialEntries: [caseDetailsPath(CASE_ID)], user: USER })

      const { user, dialog } = await openDeleteDialog()
      await user.click(confirmButton(dialog))

      expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    })
  })
})
