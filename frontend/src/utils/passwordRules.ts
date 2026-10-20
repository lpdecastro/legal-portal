import type { FormRule } from 'antd'

export const PASSWORD_MIN_LENGTH = 12
// BCrypt only uses the first 72 bytes of a password; the API rejects anything longer.
const PASSWORD_MAX_BYTES = 72

// Rules for choosing a new password, matching the API's requirements.
export function newPasswordRules(requiredMessage: string): FormRule[] {
  return [
    { required: true, message: requiredMessage },
    { min: PASSWORD_MIN_LENGTH, message: `Password must contain at least ${PASSWORD_MIN_LENGTH} characters` },
    {
      validator: (_, value: string | undefined) =>
        !value || new TextEncoder().encode(value).length <= PASSWORD_MAX_BYTES
          ? Promise.resolve()
          : Promise.reject(new Error('Password is too long')),
    },
  ]
}

// Rules for confirming the password entered in `passwordField`; list the field in the item's `dependencies`.
export function confirmPasswordRules(passwordField: string, requiredMessage: string): FormRule[] {
  return [
    { required: true, message: requiredMessage },
    ({ getFieldValue }) => ({
      validator: (_, value: string | undefined) =>
        !value || getFieldValue(passwordField) === value
          ? Promise.resolve()
          : Promise.reject(new Error('Passwords do not match')),
    }),
  ]
}
