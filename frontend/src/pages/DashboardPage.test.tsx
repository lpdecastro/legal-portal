import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse, delay } from 'msw'
import { describe, expect, it } from 'vitest'
import CaseStatistics from '../components/CaseStatistics'
import { server } from '../test/mocks/server'
import { renderWithProviders } from '../test/render'
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
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))

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
