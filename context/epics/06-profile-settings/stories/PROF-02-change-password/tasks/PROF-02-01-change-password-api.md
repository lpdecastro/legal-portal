# PROF-02-01: Change Password API

## Goal

Allow authenticated clients to securely change their password.

## Acceptance Criteria

- Current password must be correct.
- New password must be at least 12 characters and differ from the current password.
- New password and confirmation must match.
- Password is securely hashed using BCrypt.
- Previous password no longer works after a successful change.
- Returns 401 for unauthenticated requests.
- Never exposes passwords in responses or logs.
- Automated tests cover successful changes, incorrect passwords, validation errors, and unauthorized requests.

## API Contract

**PATCH** `/api/v1/clients/me/password`

**Request**

```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!",
  "confirmPassword": "NewPassword456!"
}
```

**Response — 204 No Content**

**Errors**

- `400` — Validation error
- `401` — Unauthorized
- `403` — Incorrect current password
- `500` — Internal server error

Errors follow RFC 9457 Problem Details.
