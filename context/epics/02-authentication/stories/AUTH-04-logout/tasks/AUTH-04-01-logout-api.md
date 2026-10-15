# AUTH-04-01: Logout API

**Status:** Not Started  
**Type:** Backend (BE)

## Objective

Allow authenticated clients to securely log out through the Spring Boot API.

## Scope

- Implement `POST /api/v1/auth/logout`.
- Invalidate the server-side session using Spring Security.
- Clear the session cookie.
- Require a valid CSRF token.
- Reject subsequent authenticated requests using the invalidated session.
- Add automated tests.

## Acceptance Criteria

- Authenticated clients can log out successfully.
- Successful logout returns HTTP 204.
- Server-side session is invalidated.
- Session cookie is cleared.
- Reusing the invalidated session returns HTTP 401 on protected APIs.
- Requests without a valid CSRF token are rejected.
- Automated tests pass.

## API Contract

### Request

`POST /api/v1/auth/logout`

Headers:

- `Cookie: SESSION=<session-id>`
- `X-XSRF-TOKEN: <csrf-token>`

No request body.

### Success Response (204)

No response body.

Header:
`Set-Cookie: SESSION=; Max-Age=0; Path=/; HttpOnly; Secure; SameSite=Lax`

### Error Response (403)

Missing or invalid CSRF token.

```json
{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "Invalid or missing CSRF token.",
  "errorCode": "FORBIDDEN",
  "timestamp": "2026-10-15T02:17:00Z",
  "traceId": "a1b2c3d4"
}
```

Error responses use `Content-Type: application/problem+json`.

## Out of Scope

- Logout UI.
- Login and session creation.
- Session restoration.
- Protected frontend routes.
