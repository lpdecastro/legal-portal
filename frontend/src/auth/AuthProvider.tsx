import { useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { Spin } from 'antd'
import { fetchCurrentUser } from '../api/auth'
import type { AuthenticatedUser } from '../api/auth'
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
