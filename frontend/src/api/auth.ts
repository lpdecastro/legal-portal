import { ApiError, getJson, postJson } from './client'

export interface RegisterRequest {
  firstName: string
  lastName: string
  email: string
  password: string
}

export interface RegistrationResponse {
  id: string
  email: string
  firstName: string
  lastName: string
}

export interface LoginRequest {
  email: string
  password: string
}

// The signed-in client, as returned by login and GET /auth/me.
export interface AuthenticatedUser {
  id: string
  email: string
  firstName: string
  lastName: string
}

export function register(request: RegisterRequest): Promise<RegistrationResponse> {
  return postJson<RegistrationResponse>('/auth/register', request)
}

export function login(request: LoginRequest): Promise<AuthenticatedUser> {
  return postJson<AuthenticatedUser>('/auth/login', request)
}

// Returns the client behind the session cookie, or null when there is no valid session (401).
export async function fetchCurrentUser(): Promise<AuthenticatedUser | null> {
  try {
    return await getJson<AuthenticatedUser>('/auth/me')
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null
    }
    throw error
  }
}
