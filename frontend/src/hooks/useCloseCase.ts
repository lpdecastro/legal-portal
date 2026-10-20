import { useCallback, useRef, useState } from 'react'
import { closeCase } from '../api/cases'
import type { LegalCase } from '../api/cases'

interface UseCloseCaseResult {
  // Resolves the closed case, or undefined when ignored as a duplicate.
  submit: (id: string) => Promise<LegalCase | undefined>
  closing: boolean
}

// Wraps the close case API call with a closing flag; calls made while one is in flight are ignored and resolve
// undefined. Errors propagate to the caller.
export function useCloseCase(): UseCloseCaseResult {
  const [closing, setClosing] = useState(false)
  const inFlight = useRef(false)

  const submit = useCallback(async (id: string) => {
    if (inFlight.current) return undefined
    inFlight.current = true
    setClosing(true)
    try {
      return await closeCase(id)
    } finally {
      inFlight.current = false
      setClosing(false)
    }
  }, [])

  return { submit, closing }
}
