import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse, delay } from 'msw'
import { Route, Routes, useParams } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import CaseStatistics from '../components/CaseStatistics'
import { server } from '../test/mocks/server'
import { renderWithProviders } from '../test/render'
import { paths } from '../routes/paths'
import DashboardPage from './DashboardPage'

const USER = { id: 'u1', email: 'john@example.com', firstName: 'John', lastName: 'Doe' }

const SUMMARY = {
  statistics: { total: 7, draft: 3, submitted: 2, closed: 2 },
  recentCases: [],
}

const SERVER_ERROR = () =>
  HttpResponse.json(
    { status: 500, errorCode: 'INTERNAL_ERROR', detail: 'Something went wrong.' },
    { status: 500, headers: { 'Content-Type': 'application/problem+json' } },
  )

// Serves `resolver` for the summary and counts the requests made.
function mockSummary(resolver: Parameters<typeof http.get>[1]) {
  const requests = { count: 0 }
  server.use(
    http.get('*/api/v1/dashboard/summary', (info) => {
      requests.count += 1
      return resolver(info)
    }),
  )
  return requests
}

// The count shown on the statistic card titled `label`.
async function statistic(label: string) {
  const title = await screen.findByText(label)
  return within(title.closest('.ant-card') as HTMLElement).getByText(/^\d+$/).textContent
}

const renderDashboard = () => renderWithProviders(<DashboardPage />, { user: USER })

describe('DashboardPage case statistics', () => {
  it('shows the total, draft, submitted and closed counts', async () => {
    mockSummary(() => HttpResponse.json(SUMMARY))

    renderDashboard()

    expect(await statistic('Total Cases')).toBe('7')
    expect(await statistic('Draft')).toBe('3')
    expect(await statistic('Submitted')).toBe('2')
    expect(await statistic('Closed')).toBe('2')
  })

  it('shows zero for statuses without cases', async () => {
    mockSummary(() =>
      HttpResponse.json({ statistics: { total: 0, draft: 0, submitted: 0, closed: 0 }, recentCases: [] }),
    )

    renderDashboard()

    for (const label of ['Total Cases', 'Draft', 'Submitted', 'Closed']) {
      expect(await statistic(label)).toBe('0')
    }
  })

  it('shows a loading state while the summary loads', async () => {
    mockSummary(async () => {
      await delay(50)
      return HttpResponse.json(SUMMARY)
    })

    renderDashboard()

    expect(screen.getByText('Loading statistics…')).toBeInTheDocument()
    expect(await statistic('Total Cases')).toBe('7')
    expect(screen.queryByText('Loading statistics…')).not.toBeInTheDocument()
  })

  it('shows an error with Retry when the summary fails to load, and loads it again on Retry', async () => {
    const requests = mockSummary(SERVER_ERROR)
    renderDashboard()

    expect(await screen.findByText("We couldn't load your case statistics.")).toBeInTheDocument()
    expect(requests.count).toBe(1)

    mockSummary(() => HttpResponse.json(SUMMARY))
    // Both dashboard sections show the error; either Retry reloads the shared summary.
    await userEvent.click(screen.getAllByRole('button', { name: 'Retry' })[0])

    expect(await statistic('Total Cases')).toBe('7')
    expect(screen.queryByText("We couldn't load your case statistics.")).not.toBeInTheDocument()
  })

  it('makes a single request when several components read the summary', async () => {
    const requests = mockSummary(() => HttpResponse.json(SUMMARY))

    renderWithProviders(
      <>
        <CaseStatistics />
        <CaseStatistics />
      </>,
      { user: USER },
    )

    expect(await screen.findAllByText('Total Cases')).toHaveLength(2)
    expect(requests.count).toBe(1)
  })
})

const RECENT_CASES = [
  { id: 'case-3', title: 'Lease termination', status: 'CLOSED', createdAt: '2026-10-19T12:00:00Z' },
  { id: 'case-2', title: 'Unpaid wages', status: 'SUBMITTED', createdAt: '2026-10-18T12:00:00Z' },
  { id: 'case-1', title: 'Custody dispute', status: 'DRAFT', createdAt: '2026-10-17T12:00:00Z' },
]

function CaseDetailsStub() {
  const { id } = useParams()
  return <h1>Case {id}</h1>
}

function CreateCaseStub() {
  return <h1>Create Draft Case</h1>
}

function renderDashboardRoutes() {
  return renderWithProviders(
    <Routes>
      <Route path={paths.dashboard} element={<DashboardPage />} />
      <Route path={paths.createCase} element={<CreateCaseStub />} />
      <Route path={paths.caseDetails} element={<CaseDetailsStub />} />
    </Routes>,
    { initialEntries: [paths.dashboard], user: USER },
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

// The recent case titles in the order shown.
const shownTitles = () =>
  screen.getAllByText(/^(Lease termination|Unpaid wages|Custody dispute)$/).map((element) => element.textContent)

describe('DashboardPage recent cases', () => {
  it.each([
    ['mobile', () => {}],
    ['desktop', mockDesktopScreen],
  ])('shows each case title, status and created date newest first on %s', async (_, setScreen) => {
    setScreen()
    mockSummary(() => HttpResponse.json({ ...SUMMARY, recentCases: RECENT_CASES }))

    renderDashboardRoutes()

    expect(await screen.findByText('Lease termination')).toBeInTheDocument()
    expect(shownTitles()).toEqual(['Lease termination', 'Unpaid wages', 'Custody dispute'])
    const recent = within(screen.getByText('Recent Cases').closest('.ant-card') as HTMLElement)
    expect(recent.getByText('Oct 19, 2026')).toBeInTheDocument()
    expect(recent.getByText('Closed')).toBeInTheDocument()
    expect(recent.getByText('Submitted')).toBeInTheDocument()
    expect(recent.getByText('Draft')).toBeInTheDocument()
  })

  it('opens a case when its card is clicked on mobile', async () => {
    mockSummary(() => HttpResponse.json({ ...SUMMARY, recentCases: RECENT_CASES }))
    renderDashboardRoutes()

    await userEvent.click(await screen.findByText('Unpaid wages'))

    expect(await screen.findByRole('heading', { name: 'Case case-2' })).toBeInTheDocument()
  })

  it('opens a case when its table row is clicked on desktop', async () => {
    mockDesktopScreen()
    mockSummary(() => HttpResponse.json({ ...SUMMARY, recentCases: RECENT_CASES }))
    renderDashboardRoutes()

    await userEvent.click(await screen.findByText('Oct 17, 2026'))

    expect(await screen.findByRole('heading', { name: 'Case case-1' })).toBeInTheDocument()
  })

  it('links to all cases', async () => {
    mockSummary(() => HttpResponse.json({ ...SUMMARY, recentCases: RECENT_CASES }))
    renderDashboardRoutes()

    expect(await screen.findByRole('link', { name: 'View all' })).toHaveAttribute('href', paths.cases)
  })

  it('shows an empty state with Create Case when there are no cases', async () => {
    mockSummary(() => HttpResponse.json(SUMMARY))
    renderDashboardRoutes()

    expect(await screen.findByText("You don't have any cases yet.")).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /Create Case/ }))

    expect(await screen.findByRole('heading', { name: 'Create Draft Case' })).toBeInTheDocument()
  })

  it('shows a loading state while the summary loads', async () => {
    mockSummary(async () => {
      await delay(50)
      return HttpResponse.json({ ...SUMMARY, recentCases: RECENT_CASES })
    })

    renderDashboardRoutes()

    expect(screen.getByText('Loading recent cases…')).toBeInTheDocument()
    expect(await screen.findByText('Lease termination')).toBeInTheDocument()
    expect(screen.queryByText('Loading recent cases…')).not.toBeInTheDocument()
  })

  it('shows an error with Retry when the summary fails to load, and loads it again on Retry', async () => {
    mockSummary(SERVER_ERROR)
    renderDashboardRoutes()

    expect(await screen.findByText("We couldn't load your recent cases.")).toBeInTheDocument()

    mockSummary(() => HttpResponse.json({ ...SUMMARY, recentCases: RECENT_CASES }))
    const alert = screen.getByText("We couldn't load your recent cases.").closest('.ant-alert') as HTMLElement
    await userEvent.click(within(alert).getByRole('button', { name: 'Retry' }))

    expect(await screen.findByText('Lease termination')).toBeInTheDocument()
    expect(screen.queryByText("We couldn't load your recent cases.")).not.toBeInTheDocument()
  })

  it('shares one summary request with the statistics', async () => {
    const requests = mockSummary(() => HttpResponse.json({ ...SUMMARY, recentCases: RECENT_CASES }))
    renderDashboardRoutes()

    expect(await screen.findByText('Lease termination')).toBeInTheDocument()
    expect(await statistic('Total Cases')).toBe('7')
    expect(requests.count).toBe(1)
  })
})
