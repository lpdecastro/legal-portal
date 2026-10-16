import { postJson } from './client'

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

// The signed-in client, as returned by login (and later by GET /auth/me).
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
