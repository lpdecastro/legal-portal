# AUTH-01-01: Registration API

**Status:** Not Started  
**Type:** Backend (BE)

## Objective

Allow clients to register an account through the Spring Boot API.

## Scope

- Implement `POST /api/v1/auth/register`.
- Accept first name, last name, email, and password.
- Validate required fields, email format, and password length (minimum 12 characters).
- Reject duplicate emails (case-insensitive).
- Hash passwords using BCrypt.
- Save the user and client profile in PostgreSQL.
- Return appropriate HTTP responses using RFC 9457 Problem Details for errors.
- Add automated tests.

## Acceptance Criteria

- Valid registration creates a user and client profile.
- Duplicate emails return HTTP 409.
- Invalid inputs return HTTP 400 with validation details.
- Passwords are stored as BCrypt hashes, never plaintext.
- Registration does not automatically log in the client.
- Automated tests pass.

## Out of Scope

- Registration form.
- Login and session creation.
- Email verification.

## API Contract

### Request

`POST /api/v1/auth/register`

Headers:

- `Content-Type: application/json`
- `X-XSRF-TOKEN: <csrf-token>`

Body:

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "password123456"
}
```

### Success Response (201)

Body:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Error Response (400)

```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "One or more fields contain invalid values.",
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2026-10-15T02:17:00Z",
  "traceId": "a1b2c3d4",
  "fieldErrors": [
    {
      "field": "email",
      "message": "Invalid email address"
    },
    {
      "field": "password",
      "message": "Password must contain at least 12 characters"
    }
  ]
}
```

### Error Response (409)

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Email address is already registered.",
  "errorCode": "EMAIL_ALREADY_EXISTS",
  "timestamp": "2026-10-15T02:17:00Z",
  "traceId": "a1b2c3d4"
}
```

All error responses use `Content-Type: application/problem+json`.
