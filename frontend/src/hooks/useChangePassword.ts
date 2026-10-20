import { useCallback, useRef, useState } from 'react'
import { changePassword } from '../api/profile'
import type { ChangePasswordRequest } from '../api/profile'

interface UseChangePasswordResult {
  // Resolves `false` when ignored because a change is already in flight; errors propagate to the caller.
  submit: (request: ChangePasswordRequest) => Promise<boolean>
  submitting: boolean
}

// Changes the signed-in client's password. Nothing is cached: the passwords only live in the caller's form.
export function useChangePassword(): UseChangePasswordResult {
  const [submitting, setSubmitting] = useState(false)
  // Blocks a second change that starts before the `submitting` re-render disables the form.
  const inFlightRef = useRef(false)

  const submit = useCallback(async (request: ChangePasswordRequest) => {
    if (inFlightRef.current) return false
    inFlightRef.current = true
    setSubmitting(true)
    try {
      await changePassword(request)
      return true
    } finally {
      inFlightRef.current = false
      setSubmitting(false)
    }
  }, [])

  return { submit, submitting }
}
