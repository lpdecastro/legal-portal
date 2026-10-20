import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse, delay } from 'msw'
import { Route, Routes, useParams } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { server } from '../test/mocks/server'
import { renderWithProviders } from '../test/render'
import { paths } from '../routes/paths'
import MyCasesPage from './MyCasesPage'

const USER = { id: 'u1', email: 'john@example.com', firstName: 'John', lastName: 'Doe' }

const CASES = [
  {
    id: 'case-2',
    title: 'Unpaid wages',
    category: 'Employment Law',
    status: 'SUBMITTED',
    createdAt: '2026-10-18T12:00:00Z',
  },
  {
    id: 'case-1',
    title: 'Custody dispute',
    category: 'Family Law',
    status: 'DRAFT',
    createdAt: '2026-10-17T12:00:00Z',
  },
]

const listCases = (resolver: Parameters<typeof http.get>[1]) => server.use(http.get('*/api/v1/cases', resolver))

const serverError = () =>
  HttpResponse.json(
    { status: 500, errorCode: 'INTERNAL_ERROR', detail: 'Unexpected error.' },
    { status: 500, headers: { 'Content-Type': 'application/problem+json' } },
  )

function CaseDetailsStub() {
  const { id } = useParams()
  return <h1>Case {id}</h1>
}

function CreateCaseStub() {
  return <h1>Create Draft Case</h1>
}

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path={paths.cases} element={<MyCasesPage />} />
      <Route path={paths.createCase} element={<CreateCaseStub />} />
      <Route path={paths.caseDetails} element={<CaseDetailsStub />} />
    </Routes>,
    { initialEntries: [paths.cases], user: USER },
  )
}

const originalMatchMedia = window.matchMedia

// Makes every media query match, so antd's breakpoints report a desktop screen.
function mockDesktopScreen() {
  window.matchMedia = vi.fn((query: string) => ({ ...originalMatchMedia(query), matches: true }))
}

afterEach(() => {
  window.matchMedia = originalMatchMedia
})

describe('MyCasesPage', () => {
  it('shows a loading indicator while the cases load', async () => {
    listCases(async () => {
      await delay('infinite')
      return HttpResponse.json({ content: [] })
    })

    renderPage()

    expect(await screen.findByText('Loading cases…')).toBeInTheDocument()
  })

  it("shows the client's cases in a table on desktop", async () => {
    mockDesktopScreen()
    listCases(() => HttpResponse.json({ content: CASES }))

    renderPage()

    const rows = await screen.findAllByRole('row')
    // Header row plus one row per case, newest first.
    expect(rows).toHaveLength(3)
    const first = within(rows[1])
    expect(first.getByRole('link', { name: 'Unpaid wages' })).toBeInTheDocument()
    expect(first.getByText('Employment Law')).toBeInTheDocument()
    expect(first.getByText('Submitted')).toBeInTheDocument()
    expect(first.getByText('Oct 18, 2026')).toBeInTheDocument()
    const second = within(rows[2])
    expect(second.getByText('Custody dispute')).toBeInTheDocument()
    expect(second.getByText('Family Law')).toBeInTheDocument()
    expect(second.getByText('Draft')).toBeInTheDocument()
    expect(second.getByText('Oct 17, 2026')).toBeInTheDocument()
  })

  it('opens the case details page when a table row is clicked', async () => {
    mockDesktopScreen()
    listCases(() => HttpResponse.json({ content: CASES }))
    renderPage()

    await userEvent.click(await screen.findByText('Family Law'))

    expect(await screen.findByRole('heading', { name: 'Case case-1' })).toBeInTheDocument()
  })

  it('shows the cases as linked cards on mobile and opens a case when one is clicked', async () => {
    listCases(() => HttpResponse.json({ content: CASES }))
    renderPage()

    const card = await screen.findByRole('link', { name: /custody dispute/i })
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    expect(within(card).getByText('Draft')).toBeInTheDocument()
    expect(within(card).getByText('Family Law · Oct 17, 2026')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /unpaid wages/i })).toBeInTheDocument()

    await userEvent.click(card)

    expect(await screen.findByRole('heading', { name: 'Case case-1' })).toBeInTheDocument()
  })

  it('shows an empty state with a way to create a case when there are none', async () => {
    listCases(() => HttpResponse.json({ content: [] }))
    renderPage()

    expect(await screen.findByText("You don't have any cases yet.")).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /create case/i }))

    expect(await screen.findByRole('heading', { name: 'Create Draft Case' })).toBeInTheDocument()
  })

  it('shows an error with a retry that reloads the cases', async () => {
    let calls = 0
    listCases(() => {
      calls += 1
      return calls === 1 ? serverError() : HttpResponse.json({ content: CASES })
    })
    renderPage()

    expect(await screen.findByText("We couldn't load your cases.")).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))

    expect(await screen.findByRole('link', { name: /custody dispute/i })).toBeInTheDocument()
    expect(screen.queryByText("We couldn't load your cases.")).not.toBeInTheDocument()
    expect(calls).toBe(2)
  })

  it('shows an error when the network request fails', async () => {
    listCases(() => HttpResponse.error())
    renderPage()

    expect(await screen.findByText("We couldn't load your cases.")).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument()
  })
})
