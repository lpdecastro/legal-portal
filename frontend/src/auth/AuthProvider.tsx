import { useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import type { AuthenticatedUser } from '../api/auth'
import { AuthContext } from './AuthContext'

interface AuthProviderProps {
  children: ReactNode
}

// Holds the signed-in client in memory. The session itself lives in the HttpOnly SESSION cookie.
function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<AuthenticatedUser | null>(null)
  const value = useMemo(() => ({ user, setUser }), [user])

  return <AuthContext value={value}>{children}</AuthContext>
}

export default AuthProvider
