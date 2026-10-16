import { useCallback, useState } from 'react'
import { login } from '../api/auth'
import type { AuthenticatedUser, LoginRequest } from '../api/auth'
import { useAuth } from './useAuth'

interface UseLoginResult {
  submit: (request: LoginRequest) => Promise<AuthenticatedUser>
  submitting: boolean
}

// Logs in and stores the returned user in auth state; errors propagate to the caller.
export function useLogin(): UseLoginResult {
  const { setUser } = useAuth()
  const [submitting, setSubmitting] = useState(false)

  const submit = useCallback(
    async (request: LoginRequest) => {
      setSubmitting(true)
      try {
        const user = await login(request)
        setUser(user)
        return user
      } finally {
        setSubmitting(false)
      }
    },
    [setUser],
  )

  return { submit, submitting }
}
