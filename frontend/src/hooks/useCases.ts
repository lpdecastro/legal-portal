import { useCallback, useEffect, useState } from 'react'
import { fetchCases } from '../api/cases'
import type { CaseListRequest, CaseListResponse } from '../api/cases'

interface UseCasesResult {
  // The last page loaded; kept while another page loads so the list doesn't jump.
  data?: CaseListResponse
  loading: boolean
  error: boolean
  retry: () => void
}

interface Settled {
  // Which request this result answers; any other key means a newer request is still loading.
  key: string
  data?: CaseListResponse
  error: boolean
}

// Loads one page of the signed-in client's cases, newest first, optionally of one status.
// `retry` refetches after a failure.
export function useCases({ page, size, status }: CaseListRequest): UseCasesResult {
  const [attempt, setAttempt] = useState(0)
  const [settled, setSettled] = useState<Settled>()
  const key = `${page}:${size}:${status ?? ''}:${attempt}`

  useEffect(() => {
    let cancelled = false
    fetchCases({ page, size, status })
      .then((loaded) => {
        if (!cancelled) setSettled({ key, data: loaded, error: false })
      })
      .catch(() => {
        if (!cancelled) setSettled((previous) => ({ key, data: previous?.data, error: true }))
      })
    return () => {
      cancelled = true
    }
  }, [key, page, size, status])

  const retry = useCallback(() => setAttempt((current) => current + 1), [])

  const loading = settled?.key !== key
  return { data: settled?.data, loading, error: !loading && settled.error, retry }
}
