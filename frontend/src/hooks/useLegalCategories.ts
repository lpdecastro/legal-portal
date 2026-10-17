import { useCallback, useEffect, useState } from 'react'
import { fetchLegalCategories } from '../api/legalCategories'
import type { LegalCategory } from '../api/legalCategories'

interface UseLegalCategoriesResult {
  categories: LegalCategory[]
  loading: boolean
  error: boolean
  retry: () => void
}

const byName = (a: LegalCategory, b: LegalCategory) => a.name.localeCompare(b.name)

// Loads the active legal categories, sorted by name. `retry` refetches after a failure.
export function useLegalCategories(): UseLegalCategoriesResult {
  const [categories, setCategories] = useState<LegalCategory[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    fetchLegalCategories()
      .then((loaded) => {
        if (cancelled) return
        setCategories([...loaded].sort(byName))
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

  return { categories, loading, error, retry }
}
