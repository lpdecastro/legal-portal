import { useCallback, useEffect, useState } from 'react'
import { fetchCase } from '../api/cases'
import type { LegalCase } from '../api/cases'

interface UseCaseResult {
  legalCase?: LegalCase
  loading: boolean
  // The failure from the last load, typically an `ApiError`.
  error?: unknown
  retry: () => void
}

// Loads one of the signed-in client's cases by ID. `retry` refetches after a failure.
export function useCase(id: string): UseCaseResult {
  const [legalCase, setLegalCase] = useState<LegalCase>()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>()
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    fetchCase(id)
      .then((loaded) => {
        if (cancelled) return
        setLegalCase(loaded)
        setError(undefined)
      })
      .catch((failure: unknown) => {
        if (cancelled) return
        setLegalCase(undefined)
        setError(failure)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [id, attempt])

  const retry = useCallback(() => {
    setLoading(true)
    setError(undefined)
    setAttempt((current) => current + 1)
  }, [])

  return { legalCase, loading, error, retry }
}
