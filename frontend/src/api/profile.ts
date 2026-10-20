import { getJson, patchJson } from './client'

// The signed-in client's profile. `phone` is null until set.
export interface ClientProfile {
  firstName: string
  lastName: string
  email: string
  phone: string | null
}

// Partial profile update: omitted fields stay unchanged, `null` clears the phone. Email can't be changed.
export interface UpdateProfileRequest {
  firstName?: string
  lastName?: string
  phone?: string | null
}

// Loads the signed-in client's profile.
export function fetchProfile(): Promise<ClientProfile> {
  return getJson<ClientProfile>('/clients/me')
}

// Updates the signed-in client's profile and returns it; invalid values answer 400 VALIDATION_ERROR with field errors.
export function updateProfile(request: UpdateProfileRequest): Promise<ClientProfile> {
  return patchJson<ClientProfile>('/clients/me', request)
}
