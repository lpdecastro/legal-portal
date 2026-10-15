# AUTH-02-01: Login API

**Status:** Not Started  
**Type:** Backend (BE)

## Objective

Allow registered clients to log in securely through the Spring Boot API.

## Scope

- Implement `POST /api/v1/auth/login`.
- Authenticate clients using email and password.
- Configure Spring Security authentication and filters.
- Create server-side sessions using Spring Session JDBC.
- Configure Secure, HttpOnly, and SameSite session cookies.
- Enable CSRF protection for state-changing requests.
- Reject inactive or disabled accounts.
- Return generic errors for invalid credentials using RFC 9457 Problem Details.
- Rate-limit failed login attempts.
- Add automated tests.

## Acceptance Criteria

- Valid credentials return HTTP 200 and create an authenticated session.
- Session cookie is returned after successful login.
- Subsequent authenticated API requests recognize the session.
- Invalid credentials return HTTP 401 without revealing whether the email exists.
- Inactive or disabled accounts cannot log in.
- Repeated failed login attempts are rate-limited with HTTP 429.
- Requests without valid CSRF tokens are rejected where required.
- Automated tests pass.

## Out of Scope

- Login form.
- Session restoration (`GET /auth/me`).
- Protected frontend routes.
- Logout.

## API Contract

### Request

`POST /api/v1/auth/login`

Headers:

- `Content-Type: application/json`
- `X-XSRF-TOKEN: <csrf-token>`

Body:

```json
{
  "email": "john@example.com",
  "password": "password123456"
}
```

### Success Response (200)

Header:
`Set-Cookie: SESSION=<session-id>; HttpOnly; Secure; SameSite=Lax; Path=/`

Body:

```json
{
  "id": "user-uuid",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Error Response (401)

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Invalid email or password.",
  "errorCode": "UNAUTHORIZED",
  "timestamp": "2026-10-15T02:17:00Z",
  "traceId": "a1b2c3d4"
}
```
