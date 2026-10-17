import { useCallback, useState } from 'react'
import { updateCase } from '../api/cases'
import type { LegalCase, UpdateCaseRequest } from '../api/cases'

interface UseUpdateCaseResult {
  submit: (id: string, request: UpdateCaseRequest) => Promise<LegalCase>
  submitting: boolean
}

// Wraps the update draft API call with a submitting flag; errors propagate to the caller.
export function useUpdateCase(): UseUpdateCaseResult {
  const [submitting, setSubmitting] = useState(false)

  const submit = useCallback(async (id: string, request: UpdateCaseRequest) => {
    setSubmitting(true)
    try {
      return await updateCase(id, request)
    } finally {
      setSubmitting(false)
    }
  }, [])

  return { submit, submitting }
}
