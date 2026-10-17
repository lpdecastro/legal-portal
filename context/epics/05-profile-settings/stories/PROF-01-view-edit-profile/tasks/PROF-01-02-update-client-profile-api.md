# PROF-01-02: Update Client Profile API

## Goal

Allow authenticated clients to update their personal information.

## Acceptance Criteria

- Clients can update their first name, last name, and phone number.
- Email cannot be modified.
- First name and last name are required; phone number is optional.
- Invalid inputs return validation errors.
- Only the authenticated client's profile can be updated.
- Automated tests cover successful updates, validation errors, and unauthorized requests.

## API Contract

**PATCH** `/api/v1/clients/me`

**Request**

```json
{
  "firstName": "John",
  "lastName": "Smith",
  "phone": "+639123456789"
}
```

**Response — 200 OK**

```json
{
  "firstName": "John",
  "lastName": "Smith",
  "email": "john@example.com",
  "phone": "+639123456789"
}
```

**Errors**

- `400` — Validation error
- `401` — Unauthorized
- `404` — Profile not found
- `500` — Internal server error

Errors follow RFC 9457 Problem Details.
