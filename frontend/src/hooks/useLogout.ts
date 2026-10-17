import { useCallback, useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import { logout } from '../api/auth'
import { ApiError } from '../api/client'
import { paths } from '../routes/paths'
import { useAuth } from './useAuth'

interface UseLogoutResult {
  submit: () => Promise<void>
  loggingOut: boolean
}

// Ends the session, then clears auth state and goes to the login page. Other errors propagate to the caller and leave
// the client signed in, since the session may still be valid on the server.
export function useLogout(): UseLogoutResult {
  const { setUser } = useAuth()
  const navigate = useNavigate()
  const [loggingOut, setLoggingOut] = useState(false)
  const inFlight = useRef(false)

  const submit = useCallback(async () => {
    if (inFlight.current) return
    inFlight.current = true
    setLoggingOut(true)
    try {
      await logout()
    } catch (error) {
      // 401: the session had already ended, so the client is signed out either way.
      if (!(error instanceof ApiError && error.status === 401)) {
        throw error
      }
    } finally {
      inFlight.current = false
      setLoggingOut(false)
    }
    setUser(null)
    navigate(paths.login, { replace: true })
  }, [navigate, setUser])

  return { submit, loggingOut }
}
