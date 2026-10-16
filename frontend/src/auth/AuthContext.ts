import { createContext } from 'react'
import type { AuthenticatedUser } from '../api/auth'

export interface AuthContextValue {
  user: AuthenticatedUser | null
  setUser: (user: AuthenticatedUser | null) => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
