import { screen, waitFor, within } from '@testing-library/react'
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

// A page of the cases API response; totals default to a single page holding `content`.
function casePage(content: typeof CASES, page: Partial<{ size: number; number: number; totalElements: number }> = {}) {
  const size = page.size ?? 10
  const totalElements = page.totalElements ?? content.length
  return HttpResponse.json({
    content,
    page: { size, number: page.number ?? 0, totalElements, totalPages: Math.ceil(totalElements / size) },
  })
}

// `count` cases titled "Case 1"…"Case N", newest first.
const manyCases = (count: number) =>
  Array.from({ length: count }, (_, index) => ({
    id: `case-${index + 1}`,
    title: `Case ${index + 1}`,
    category: 'Family Law',
    status: 'DRAFT',
    createdAt: '2026-10-17T12:00:00Z',
  }))

// Serves `all` cases page by page, honouring the `page` and `size` query parameters, and records each request.
function servePages(all: typeof CASES) {
  const requests: { page: number; size: number }[] = []
  listCases(({ request }) => {
    const params = new URL(request.url).searchParams
    const page = Number(params.get('page'))
    const size = Number(params.get('size'))
    requests.push({ page, size })
    return casePage(all.slice(page * size, (page + 1) * size), { size, number: page, totalElements: all.length })
  })
  return requests
}

// Serves `all` cases filtered by the `status` query parameter and paged, and records each request
// (`status` is null when the parameter is left out).
function serveFiltered(all: typeof CASES) {
  const requests: { page: number; size: number; status: string | null }[] = []
  listCases(({ request }) => {
    const params = new URL(request.url).searchParams
    const page = Number(params.get('page'))
    const size = Number(params.get('size'))
    const status = params.get('status')
    requests.push({ page, size, status })
    const matching = status ? all.filter((legalCase) => legalCase.status === status) : all
    return casePage(matching.slice(page * size, (page + 1) * size), {
      size,
      number: page,
      totalElements: matching.length,
    })
  })
  return requests
}

// `count` cases in one status titled e.g. "Draft 1"…"Draft N".
const casesInStatus = (status: string, label: string, count: number) =>
  Array.from({ length: count }, (_, index) => ({
    id: `${status.toLowerCase()}-${index + 1}`,
    title: `${label} ${index + 1}`,
    category: 'Family Law',
    status,
    createdAt: '2026-10-17T12:00:00Z',
  }))

// 25 drafts, 3 submitted and 2 closed cases.
const MIXED_CASES = [
  ...casesInStatus('DRAFT', 'Draft', 25),
  ...casesInStatus('SUBMITTED', 'Submitted', 3),
  ...casesInStatus('CLOSED', 'Closed', 2),
]

// Serves `all` cases filtered by the `status` and `search` (case-insensitive title match) query parameters and paged,
// and records each request (`status` and `search` are null when left out).
function serveSearchable(all: typeof CASES) {
  const requests: { page: number; size: number; status: string | null; search: string | null }[] = []
  listCases(({ request }) => {
    const params = new URL(request.url).searchParams
    const page = Number(params.get('page'))
    const size = Number(params.get('size'))
    const status = params.get('status')
    const search = params.get('search')
    requests.push({ page, size, status, search })
    const matching = all.filter(
      (legalCase) =>
        (!status || legalCase.status === status) &&
        (!search || legalCase.title.toLowerCase().includes(search.toLowerCase())),
    )
    return casePage(matching.slice(page * size, (page + 1) * size), {
      size,
      number: page,
      totalElements: matching.length,
    })
  })
  return requests
}

const searchBox = () => screen.getByRole('textbox', { name: 'Search cases by title' })

// Clicks the search box's clear button.
async function clearSearch() {
  const clear = searchBox().closest('.ant-input-affix-wrapper')?.querySelector('.ant-input-clear-icon')
  if (!clear) throw new Error('No clear button')
  await userEvent.click(clear)
}

// Picks an option in the status filter by its label.
async function selectStatus(label: string) {
  await userEvent.click(screen.getByRole('combobox', { name: 'Filter by status' }))
  // antd's hidden accessible listbox only lists raw values, so click the visible dropdown item.
  const option = await waitFor(() => {
    const item = document.querySelector(`.ant-select-item-option[title="${label}"]`)
    if (!item) throw new Error(`No "${label}" option`)
    return item
  })
  await userEvent.click(option)
}

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
      return casePage([])
    })

    renderPage()

    expect(await screen.findByText('Loading cases…')).toBeInTheDocument()
  })

  it("shows the client's cases in a table on desktop", async () => {
    mockDesktopScreen()
    listCases(() => casePage(CASES))

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
    listCases(() => casePage(CASES))
    renderPage()

    await userEvent.click(await screen.findByText('Family Law'))

    expect(await screen.findByRole('heading', { name: 'Case case-1' })).toBeInTheDocument()
  })

  it('shows the cases as linked cards on mobile and opens a case when one is clicked', async () => {
    listCases(() => casePage(CASES))
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
    listCases(() => casePage([]))
    renderPage()

    expect(await screen.findByText("You don't have any cases yet.")).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /create case/i }))

    expect(await screen.findByRole('heading', { name: 'Create Draft Case' })).toBeInTheDocument()
  })

  it('shows an error with a retry that reloads the cases', async () => {
    let calls = 0
    listCases(() => {
      calls += 1
      return calls === 1 ? serverError() : casePage(CASES)
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

  describe('pagination', () => {
    it('requests the first page of 10 cases and shows the page summary from the API', async () => {
      const requests = servePages(manyCases(25))
      renderPage()

      expect(await screen.findByText('Page 1 of 3 · 25 cases')).toBeInTheDocument()
      expect(screen.getByRole('link', { name: /^case 1\b/i })).toBeInTheDocument()
      expect(screen.getByRole('link', { name: /^case 10\b/i })).toBeInTheDocument()
      expect(screen.queryByRole('link', { name: /^case 11\b/i })).not.toBeInTheDocument()
      expect(screen.getByTitle('3')).toBeInTheDocument()
      expect(screen.queryByTitle('4')).not.toBeInTheDocument()
      expect(requests).toEqual([{ page: 0, size: 10 }])
    })

    it('moves between pages and disables previous/next at the ends', async () => {
      const requests = servePages(manyCases(25))
      renderPage()

      await screen.findByText('Page 1 of 3 · 25 cases')
      expect(screen.getByTitle('Previous Page')).toHaveAttribute('aria-disabled', 'true')
      expect(screen.getByTitle('Next Page')).toHaveAttribute('aria-disabled', 'false')

      await userEvent.click(screen.getByTitle('Next Page'))
      expect(await screen.findByText('Page 2 of 3 · 25 cases')).toBeInTheDocument()
      expect(screen.getByRole('link', { name: /^case 11\b/i })).toBeInTheDocument()
      expect(screen.queryByRole('link', { name: /^case 1\b/i })).not.toBeInTheDocument()
      expect(screen.getByTitle('Previous Page')).toHaveAttribute('aria-disabled', 'false')

      await userEvent.click(screen.getByTitle('3'))
      expect(await screen.findByText('Page 3 of 3 · 25 cases')).toBeInTheDocument()
      expect(screen.getByRole('link', { name: /^case 25\b/i })).toBeInTheDocument()
      expect(screen.getByTitle('Next Page')).toHaveAttribute('aria-disabled', 'true')

      await userEvent.click(screen.getByTitle('Previous Page'))
      expect(await screen.findByText('Page 2 of 3 · 25 cases')).toBeInTheDocument()

      expect(requests.map(({ page }) => page)).toEqual([0, 1, 2, 1])
    })

    it('disables previous and next when there is only one page', async () => {
      listCases(() => casePage(CASES))
      renderPage()

      expect(await screen.findByText('Page 1 of 1 · 2 cases')).toBeInTheDocument()
      expect(screen.getByTitle('Previous Page')).toHaveAttribute('aria-disabled', 'true')
      expect(screen.getByTitle('Next Page')).toHaveAttribute('aria-disabled', 'true')
    })

    it('changes the page size, showing more cases and going back to the first page', async () => {
      const requests = servePages(manyCases(45))
      renderPage()

      await screen.findByText('Page 1 of 5 · 45 cases')
      await userEvent.click(screen.getByTitle('3'))
      await screen.findByText('Page 3 of 5 · 45 cases')

      await userEvent.click(screen.getByRole('combobox', { name: 'Cases per page' }))
      await userEvent.click(await screen.findByText('20 / page'))

      expect(await screen.findByText('Page 1 of 3 · 45 cases')).toBeInTheDocument()
      expect(screen.getByRole('link', { name: /^case 1\b/i })).toBeInTheDocument()
      expect(screen.getByRole('link', { name: /^case 20\b/i })).toBeInTheDocument()
      expect(screen.queryByRole('link', { name: /^case 21\b/i })).not.toBeInTheDocument()
      expect(requests.at(-1)).toEqual({ page: 0, size: 20 })
    })

    it('offers page sizes of 10, 20 and 50', async () => {
      servePages(manyCases(25))
      renderPage()

      await screen.findByText('Page 1 of 3 · 25 cases')
      await userEvent.click(screen.getByRole('combobox', { name: 'Cases per page' }))

      // antd's hidden accessible listbox only lists raw values, so read the visible dropdown items.
      await screen.findByText('50 / page')
      const options = document.querySelectorAll('.ant-select-item-option')
      expect([...options].map((option) => option.textContent)).toEqual(['10 / page', '20 / page', '50 / page'])
    })

    it('keeps the current cases and disables the controls while the next page loads', async () => {
      const all = manyCases(25)
      const requests: number[] = []
      listCases(async ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page'))
        requests.push(page)
        if (page > 0) await delay('infinite')
        return casePage(all.slice(0, 10), { totalElements: 25 })
      })
      renderPage()

      await screen.findByText('Page 1 of 3 · 25 cases')
      await userEvent.click(screen.getByTitle('Next Page'))

      expect(screen.getByRole('link', { name: /^case 1\b/i })).toBeInTheDocument()
      // antd marks the whole control disabled; its items' aria-disabled only reflects whether a page exists.
      expect(screen.getByTitle('2').closest('ul')).toHaveClass('ant-pagination-disabled')
      await userEvent.click(screen.getByTitle('3'))
      expect(requests).toEqual([0, 1])
    })

    it('shows an error when a page fails to load and retries that page', async () => {
      const all = manyCases(25)
      const pages: number[] = []
      listCases(({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page'))
        pages.push(page)
        if (page === 1 && pages.filter((requested) => requested === 1).length === 1) return serverError()
        return casePage(all.slice(page * 10, page * 10 + 10), { number: page, totalElements: 25 })
      })
      renderPage()

      await screen.findByText('Page 1 of 3 · 25 cases')
      await userEvent.click(screen.getByTitle('Next Page'))

      expect(await screen.findByText("We couldn't load your cases.")).toBeInTheDocument()

      await userEvent.click(screen.getByRole('button', { name: 'Retry' }))

      expect(await screen.findByText('Page 2 of 3 · 25 cases')).toBeInTheDocument()
      expect(pages).toEqual([0, 1, 1])
    })

    it('moves back to the last page when the requested page is past the end', async () => {
      servePages(manyCases(11))
      renderPage()

      await screen.findByText('Page 1 of 2 · 11 cases')
      // The 11th case is deleted elsewhere before page 2 is requested.
      const requests = servePages(manyCases(10))
      await userEvent.click(screen.getByTitle('Next Page'))

      expect(await screen.findByText('Page 1 of 1 · 10 cases')).toBeInTheDocument()
      expect(screen.getByRole('link', { name: /^case 10\b/i })).toBeInTheDocument()
      expect(requests).toEqual([
        { page: 1, size: 10 },
        { page: 0, size: 10 },
      ])
    })

    it('shows the pagination controls below the cases on desktop', async () => {
      mockDesktopScreen()
      servePages(manyCases(25))
      renderPage()

      const table = await screen.findByRole('table')
      const summary = screen.getByText('Page 1 of 3 · 25 cases')
      expect(table.compareDocumentPosition(summary) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    })

    it('shows no pagination controls when there are no cases', async () => {
      listCases(() => casePage([]))
      renderPage()

      await screen.findByText("You don't have any cases yet.")
      expect(screen.queryByRole('list')).not.toBeInTheDocument()
      expect(screen.queryByText(/^Page \d/)).not.toBeInTheDocument()
    })
  })

  describe('status filter', () => {
    it('offers All Statuses, Draft, Submitted and Closed, starting with All Statuses', async () => {
      const requests = serveFiltered(MIXED_CASES)
      renderPage()

      await screen.findByText('Page 1 of 3 · 30 cases')
      const filter = screen.getByRole('combobox', { name: 'Filter by status' })
      expect(filter.closest('.ant-select')).toHaveTextContent('All Statuses')
      await userEvent.click(filter)

      await waitFor(() => expect(document.querySelectorAll('.ant-select-item-option')).toHaveLength(4))
      const options = document.querySelectorAll('.ant-select-item-option')
      expect([...options].map((option) => option.textContent)).toEqual(['All Statuses', 'Draft', 'Submitted', 'Closed'])
      expect(requests).toEqual([{ page: 0, size: 10, status: null }])
    })

    it.each([
      ['Draft', 'DRAFT', 'Page 1 of 3 · 25 cases', /^draft 1\b/i],
      ['Submitted', 'SUBMITTED', 'Page 1 of 1 · 3 cases', /^submitted 1\b/i],
      ['Closed', 'CLOSED', 'Page 1 of 1 · 2 cases', /^closed 1\b/i],
    ])('shows only %s cases when that status is selected', async (label, status, summary, firstCase) => {
      const requests = serveFiltered(MIXED_CASES)
      renderPage()

      await screen.findByText('Page 1 of 3 · 30 cases')
      await selectStatus(label)

      expect(await screen.findByText(summary)).toBeInTheDocument()
      expect(screen.getByRole('link', { name: firstCase })).toBeInTheDocument()
      const shown = screen.getAllByRole('link').map((link) => link.textContent ?? '')
      expect(shown.every((text) => text.startsWith(label))).toBe(true)
      expect(requests.at(-1)).toEqual({ page: 0, size: 10, status })
    })

    it('keeps the status filter when changing pages and page size', async () => {
      const requests = serveFiltered(MIXED_CASES)
      renderPage()

      await screen.findByText('Page 1 of 3 · 30 cases')
      await selectStatus('Draft')
      await screen.findByText('Page 1 of 3 · 25 cases')

      await userEvent.click(screen.getByTitle('Next Page'))
      expect(await screen.findByText('Page 2 of 3 · 25 cases')).toBeInTheDocument()
      expect(screen.getByRole('link', { name: /^draft 11\b/i })).toBeInTheDocument()

      await userEvent.click(screen.getByRole('combobox', { name: 'Cases per page' }))
      await userEvent.click(await screen.findByText('20 / page'))
      expect(await screen.findByText('Page 1 of 2 · 25 cases')).toBeInTheDocument()

      expect(requests.slice(1)).toEqual([
        { page: 0, size: 10, status: 'DRAFT' },
        { page: 1, size: 10, status: 'DRAFT' },
        { page: 0, size: 20, status: 'DRAFT' },
      ])
    })

    it('goes back to the first page when the filter changes, keeping the page size', async () => {
      const requests = serveFiltered(MIXED_CASES)
      renderPage()

      await screen.findByText('Page 1 of 3 · 30 cases')
      await userEvent.click(screen.getByTitle('3'))
      await screen.findByText('Page 3 of 3 · 30 cases')

      await selectStatus('Draft')

      expect(await screen.findByText('Page 1 of 3 · 25 cases')).toBeInTheDocument()
      expect(requests.at(-1)).toEqual({ page: 0, size: 10, status: 'DRAFT' })
    })

    it('removes the status filter when All Statuses is selected', async () => {
      const requests = serveFiltered(MIXED_CASES)
      renderPage()

      await screen.findByText('Page 1 of 3 · 30 cases')
      await selectStatus('Closed')
      await screen.findByText('Page 1 of 1 · 2 cases')

      await selectStatus('All Statuses')

      expect(await screen.findByText('Page 1 of 3 · 30 cases')).toBeInTheDocument()
      expect(screen.getByRole('link', { name: /^draft 1\b/i })).toBeInTheDocument()
      expect(requests.at(-1)).toEqual({ page: 0, size: 10, status: null })
    })

    it('shows an empty state when no cases match, keeping the filter and offering all statuses', async () => {
      const requests = serveFiltered(MIXED_CASES.filter(({ status }) => status !== 'CLOSED'))
      renderPage()

      await screen.findByText('Page 1 of 3 · 28 cases')
      await selectStatus('Closed')

      expect(await screen.findByText('No closed cases found.')).toBeInTheDocument()
      expect(screen.getByRole('combobox', { name: 'Filter by status' })).toBeInTheDocument()
      expect(screen.queryByText(/^Page \d/)).not.toBeInTheDocument()
      expect(screen.queryByText("You don't have any cases yet.")).not.toBeInTheDocument()

      await userEvent.click(screen.getByRole('button', { name: 'Show All Statuses' }))

      expect(await screen.findByText('Page 1 of 3 · 28 cases')).toBeInTheDocument()
      expect(requests.at(-1)).toEqual({ page: 0, size: 10, status: null })
    })

    it('shows an error when the filtered cases fail to load and retries with the same filter', async () => {
      const statuses: (string | null)[] = []
      listCases(({ request }) => {
        const status = new URL(request.url).searchParams.get('status')
        statuses.push(status)
        if (status === 'SUBMITTED' && statuses.filter((requested) => requested === status).length === 1) {
          return serverError()
        }
        return casePage(status ? CASES.filter((legalCase) => legalCase.status === status) : CASES)
      })
      renderPage()

      await screen.findByText('Page 1 of 1 · 2 cases')
      await selectStatus('Submitted')

      expect(await screen.findByText("We couldn't load your cases.")).toBeInTheDocument()
      expect(screen.getByRole('combobox', { name: 'Filter by status' })).toBeInTheDocument()

      await userEvent.click(screen.getByRole('button', { name: 'Retry' }))

      expect(await screen.findByText('Page 1 of 1 · 1 case')).toBeInTheDocument()
      expect(screen.getByRole('link', { name: /unpaid wages/i })).toBeInTheDocument()
      expect(statuses).toEqual([null, 'SUBMITTED', 'SUBMITTED'])
    })

    it('shows the filter above the cases table on desktop', async () => {
      mockDesktopScreen()
      serveFiltered(MIXED_CASES)
      renderPage()

      const table = await screen.findByRole('table')
      const filter = screen.getByRole('combobox', { name: 'Filter by status' })
      expect(filter.compareDocumentPosition(table) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    })

    it('shows no filter when the client has no cases at all', async () => {
      listCases(() => casePage([]))
      renderPage()

      await screen.findByText("You don't have any cases yet.")
      expect(screen.queryByRole('combobox', { name: 'Filter by status' })).not.toBeInTheDocument()
    })
  })

  describe('search', () => {
    it('shows only the cases whose title contains the search, case-insensitively', async () => {
      const requests = serveSearchable(MIXED_CASES)
      renderPage()

      await screen.findByText('Page 1 of 3 · 30 cases')
      await userEvent.type(searchBox(), 'SUBMIT')

      expect(await screen.findByText('Page 1 of 1 · 3 cases')).toBeInTheDocument()
      const shown = screen.getAllByRole('link').map((link) => link.textContent ?? '')
      expect(shown).toHaveLength(3)
      expect(shown.every((text) => text.startsWith('Submitted'))).toBe(true)
      expect(requests.at(-1)).toEqual({ page: 0, size: 10, status: null, search: 'SUBMIT' })
    })

    it('waits for typing to pause before searching, sending one request', async () => {
      const requests = serveSearchable(MIXED_CASES)
      renderPage()

      await screen.findByText('Page 1 of 3 · 30 cases')
      await userEvent.type(searchBox(), 'closed')

      expect(await screen.findByText('Page 1 of 1 · 2 cases')).toBeInTheDocument()
      expect(requests.map(({ search }) => search)).toEqual([null, 'closed'])
    })

    it('ignores surrounding whitespace and does not search for whitespace alone', async () => {
      const requests = serveSearchable(MIXED_CASES)
      renderPage()

      await screen.findByText('Page 1 of 3 · 30 cases')
      await userEvent.type(searchBox(), '   ')
      await new Promise((resolve) => setTimeout(resolve, 400))
      expect(requests).toHaveLength(1)

      await userEvent.type(searchBox(), 'closed  ')

      expect(await screen.findByText('Page 1 of 1 · 2 cases')).toBeInTheDocument()
      expect(requests.at(-1)?.search).toBe('closed')
    })

    it('restores every case when the search is cleared', async () => {
      const requests = serveSearchable(MIXED_CASES)
      renderPage()

      await screen.findByText('Page 1 of 3 · 30 cases')
      await userEvent.type(searchBox(), 'closed')
      await screen.findByText('Page 1 of 1 · 2 cases')

      await clearSearch()

      expect(await screen.findByText('Page 1 of 3 · 30 cases')).toBeInTheDocument()
      expect(searchBox()).toHaveValue('')
      expect(requests.at(-1)).toEqual({ page: 0, size: 10, status: null, search: null })
    })

    it('goes back to the first page when searching, keeping the page size', async () => {
      const requests = serveSearchable(MIXED_CASES)
      renderPage()

      await screen.findByText('Page 1 of 3 · 30 cases')
      await userEvent.click(screen.getByTitle('3'))
      await screen.findByText('Page 3 of 3 · 30 cases')

      await userEvent.type(searchBox(), 'draft')

      expect(await screen.findByText('Page 1 of 3 · 25 cases')).toBeInTheDocument()
      expect(requests.at(-1)).toEqual({ page: 0, size: 10, status: null, search: 'draft' })
    })

    it('combines the search with the status filter and keeps both when changing pages', async () => {
      const requests = serveSearchable([...MIXED_CASES, ...casesInStatus('SUBMITTED', 'Draft review', 2)])
      renderPage()

      await screen.findByText('Page 1 of 4 · 32 cases')
      await userEvent.type(searchBox(), 'draft')
      await screen.findByText('Page 1 of 3 · 27 cases')

      await selectStatus('Draft')
      expect(await screen.findByText('Page 1 of 3 · 25 cases')).toBeInTheDocument()

      await userEvent.click(screen.getByTitle('Next Page'))
      expect(await screen.findByText('Page 2 of 3 · 25 cases')).toBeInTheDocument()
      expect(screen.getByRole('link', { name: /^draft 11\b/i })).toBeInTheDocument()

      await userEvent.click(screen.getByRole('combobox', { name: 'Cases per page' }))
      await userEvent.click(await screen.findByText('20 / page'))
      expect(await screen.findByText('Page 1 of 2 · 25 cases')).toBeInTheDocument()

      await selectStatus('Submitted')
      expect(await screen.findByText('Page 1 of 1 · 2 cases')).toBeInTheDocument()
      expect(searchBox()).toHaveValue('draft')

      expect(requests.slice(2)).toEqual([
        { page: 0, size: 10, status: 'DRAFT', search: 'draft' },
        { page: 1, size: 10, status: 'DRAFT', search: 'draft' },
        { page: 0, size: 20, status: 'DRAFT', search: 'draft' },
        { page: 0, size: 20, status: 'SUBMITTED', search: 'draft' },
      ])
    })

    it('shows an empty state when no cases match, keeping the search and offering to clear it', async () => {
      const requests = serveSearchable(MIXED_CASES)
      renderPage()

      await screen.findByText('Page 1 of 3 · 30 cases')
      await userEvent.type(searchBox(), 'tenancy')

      expect(await screen.findByText('No cases match "tenancy".')).toBeInTheDocument()
      expect(searchBox()).toHaveValue('tenancy')
      expect(screen.getByRole('combobox', { name: 'Filter by status' })).toBeInTheDocument()
      expect(screen.queryByText(/^Page \d/)).not.toBeInTheDocument()
      expect(screen.queryByText("You don't have any cases yet.")).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Show All Statuses' })).not.toBeInTheDocument()

      await userEvent.click(screen.getByRole('button', { name: 'Clear Search' }))

      expect(await screen.findByText('Page 1 of 3 · 30 cases')).toBeInTheDocument()
      expect(searchBox()).toHaveValue('')
      expect(requests.at(-1)).toEqual({ page: 0, size: 10, status: null, search: null })
    })

    it('names the status in the empty state and offers to clear either filter', async () => {
      serveSearchable(MIXED_CASES)
      renderPage()

      await screen.findByText('Page 1 of 3 · 30 cases')
      await selectStatus('Closed')
      await screen.findByText('Page 1 of 1 · 2 cases')
      await userEvent.type(searchBox(), 'draft')

      expect(await screen.findByText('No closed cases match "draft".')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Clear Search' })).toBeInTheDocument()

      await userEvent.click(screen.getByRole('button', { name: 'Show All Statuses' }))

      expect(await screen.findByText('Page 1 of 3 · 25 cases')).toBeInTheDocument()
      expect(searchBox()).toHaveValue('draft')
    })

    it('shows an error when the search fails and retries the same search', async () => {
      const searches: (string | null)[] = []
      listCases(({ request }) => {
        const search = new URL(request.url).searchParams.get('search')
        searches.push(search)
        if (search && searches.filter((requested) => requested === search).length === 1) return serverError()
        return casePage(search ? CASES.filter(({ title }) => title.toLowerCase().includes(search)) : CASES)
      })
      renderPage()

      await screen.findByText('Page 1 of 1 · 2 cases')
      await userEvent.type(searchBox(), 'wages')

      expect(await screen.findByText("We couldn't load your cases.")).toBeInTheDocument()
      expect(searchBox()).toHaveValue('wages')

      await userEvent.click(screen.getByRole('button', { name: 'Retry' }))

      expect(await screen.findByText('Page 1 of 1 · 1 case')).toBeInTheDocument()
      expect(screen.getByRole('link', { name: /unpaid wages/i })).toBeInTheDocument()
      expect(searches).toEqual([null, 'wages', 'wages'])
    })

    it('shows the search box above the cases table on desktop', async () => {
      mockDesktopScreen()
      serveSearchable(MIXED_CASES)
      renderPage()

      const table = await screen.findByRole('table')
      expect(searchBox().compareDocumentPosition(table) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    })

    it('shows no search box when the client has no cases at all', async () => {
      listCases(() => casePage([]))
      renderPage()

      await screen.findByText("You don't have any cases yet.")
      expect(screen.queryByRole('textbox', { name: 'Search cases by title' })).not.toBeInTheDocument()
    })
  })
})
