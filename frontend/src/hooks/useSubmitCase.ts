import { useCallback, useRef, useState } from 'react'
import { submitCase } from '../api/cases'
import type { LegalCase } from '../api/cases'

interface UseSubmitCaseResult {
  // Resolves the submitted case, or undefined when ignored as a duplicate.
  submit: (id: string) => Promise<LegalCase | undefined>
  submitting: boolean
}

// Wraps the submit case API call with a submitting flag; calls made while one is in flight are ignored and resolve
// undefined. Errors propagate to the caller.
export function useSubmitCase(): UseSubmitCaseResult {
  const [submitting, setSubmitting] = useState(false)
  const inFlight = useRef(false)

  const submit = useCallback(async (id: string) => {
    if (inFlight.current) return undefined
    inFlight.current = true
    setSubmitting(true)
    try {
      return await submitCase(id)
    } finally {
      inFlight.current = false
      setSubmitting(false)
    }
  }, [])

  return { submit, submitting }
}
