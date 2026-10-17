import { useCallback, useState } from 'react'
import { createCase } from '../api/cases'
import type { CreateCaseRequest, LegalCase } from '../api/cases'

interface UseCreateCaseResult {
  submit: (request: CreateCaseRequest) => Promise<LegalCase>
  submitting: boolean
}

// Wraps the create draft API call with a submitting flag; errors propagate to the caller.
export function useCreateCase(): UseCreateCaseResult {
  const [submitting, setSubmitting] = useState(false)

  const submit = useCallback(async (request: CreateCaseRequest) => {
    setSubmitting(true)
    try {
      return await createCase(request)
    } finally {
      setSubmitting(false)
    }
  }, [])

  return { submit, submitting }
}
