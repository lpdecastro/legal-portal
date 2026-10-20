import { useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { Spin } from 'antd'
import { useQueryClient } from '@tanstack/react-query'
import { fetchCurrentUser } from '../api/auth'
import type { AuthenticatedUser } from '../api/auth'
import { onUnauthorized } from '../api/client'
import { AuthContext } from './AuthContext'

interface AuthProviderProps {
  children: ReactNode
  // Skips session restoration and starts with this user (null = signed out). Used by tests.
  initialUser?: AuthenticatedUser | null
}

// Holds the signed-in client in memory. The session itself lives in the HttpOnly SESSION cookie, so on start-up
// the client is restored from GET /auth/me before anything else renders.
function AuthProvider({ children, initialUser }: AuthProviderProps) {
  const [user, setUser] = useState<AuthenticatedUser | null>(initialUser ?? null)
  const [restoring, setRestoring] = useState(initialUser === undefined)
  const value = useMemo(() => ({ user, setUser }), [user])
  const queryClient = useQueryClient()

  // Cached responses belong to the client who loaded them, so drop them on sign-out.
  useEffect(() => {
    if (!user) queryClient.clear()
  }, [user, queryClient])

  // Any 401 means the session is gone (expired, ended on the server, or never existed), so sign the client out.
  // Protected routes then redirect to the login page.
  useEffect(() => onUnauthorized(() => setUser(null)), [])

  useEffect(() => {
    if (!restoring) return

    let cancelled = false
    fetchCurrentUser()
      // Any failure leaves the client signed out; only a confirmed session restores them.
      .catch(() => null)
      .then((restoredUser) => {
        if (cancelled) return
        setUser(restoredUser)
        setRestoring(false)
      })
    return () => {
      cancelled = true
    }
  }, [restoring])

  if (restoring) {
    return <Spin fullscreen size="large" description="Loading…" />
  }

  return <AuthContext value={value}>{children}</AuthContext>
}

export default AuthProvider
