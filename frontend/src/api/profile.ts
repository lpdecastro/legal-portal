import { getJson, patchJson, patchNoContent } from './client'

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

// Passwords are sent as entered, never trimmed.
export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

// Loads the signed-in client's profile.
export function fetchProfile(): Promise<ClientProfile> {
  return getJson<ClientProfile>('/clients/me')
}

// Updates the signed-in client's profile and returns it; invalid values answer 400 VALIDATION_ERROR with field errors.
export function updateProfile(request: UpdateProfileRequest): Promise<ClientProfile> {
  return patchJson<ClientProfile>('/clients/me', request)
}

// Changes the signed-in client's password; the session stays signed in. A wrong current password answers 403
// INCORRECT_PASSWORD, invalid values 400 VALIDATION_ERROR with field errors.
export function changePassword(request: ChangePasswordRequest): Promise<void> {
  return patchNoContent('/clients/me/password', request)
}
