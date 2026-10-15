# PROF-01-01: Get Client Profile API

## Goal

Allow authenticated clients to retrieve their own profile information.

## Acceptance Criteria

- Returns the authenticated client's profile.
- Includes first name, last name, email, and phone number.
- Returns 401 for unauthenticated requests.
- Does not expose sensitive information.
- Automated tests cover successful and unauthorized requests.

## API Contract

**GET** `/api/v1/clients/me`

**Response — 200 OK**

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "phone": "+639123456789"
}
```

**Errors**

- `401` — Unauthorized
- `404` — Profile not found
- `500` — Internal server error

Errors follow RFC 9457 Problem Details.
