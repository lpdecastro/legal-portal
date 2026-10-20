import { useCallback, useRef, useState } from 'react'
import { deleteCase } from '../api/cases'

interface UseDeleteCaseResult {
  // Resolves true once the case is deleted, false when ignored as a duplicate.
  submit: (id: string) => Promise<boolean>
  deleting: boolean
}

// Wraps the delete draft API call with a deleting flag; calls made while one is in flight are ignored and resolve
// false. Errors propagate to the caller.
export function useDeleteCase(): UseDeleteCaseResult {
  const [deleting, setDeleting] = useState(false)
  const inFlight = useRef(false)

  const submit = useCallback(async (id: string) => {
    if (inFlight.current) return false
    inFlight.current = true
    setDeleting(true)
    try {
      await deleteCase(id)
      return true
    } finally {
      inFlight.current = false
      setDeleting(false)
    }
  }, [])

  return { submit, deleting }
}
