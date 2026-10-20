import { useCallback, useEffect, useState } from 'react'
import { fetchCases } from '../api/cases'
import type { CaseSummary } from '../api/cases'

interface UseCasesResult {
  cases: CaseSummary[]
  loading: boolean
  error: boolean
  retry: () => void
}

// Loads the signed-in client's cases, newest first. `retry` refetches after a failure.
export function useCases(): UseCasesResult {
  const [cases, setCases] = useState<CaseSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    fetchCases()
      .then((loaded) => {
        if (cancelled) return
        setCases(loaded)
        setError(false)
      })
      .catch(() => {
        if (cancelled) return
        setError(true)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [attempt])

  const retry = useCallback(() => {
    setLoading(true)
    setError(false)
    setAttempt((current) => current + 1)
  }, [])

  return { cases, loading, error, retry }
}
