import { useCallback, useState } from 'react'
import { register } from '../api/auth'
import type { RegisterRequest, RegistrationResponse } from '../api/auth'

interface UseRegisterResult {
  submit: (request: RegisterRequest) => Promise<RegistrationResponse>
  submitting: boolean
}

// Wraps the register API call with a submitting flag; errors propagate to the caller.
export function useRegister(): UseRegisterResult {
  const [submitting, setSubmitting] = useState(false)

  const submit = useCallback(async (request: RegisterRequest) => {
    setSubmitting(true)
    try {
      return await register(request)
    } finally {
      setSubmitting(false)
    }
  }, [])

  return { submit, submitting }
}
