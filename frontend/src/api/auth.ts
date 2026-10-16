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

export function register(request: RegisterRequest): Promise<RegistrationResponse> {
  return postJson<RegistrationResponse>('/auth/register', request)
}
