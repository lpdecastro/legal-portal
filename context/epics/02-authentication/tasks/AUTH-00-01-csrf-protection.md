# AUTH-00-01: CSRF Protection

**Status:** Not Started  
**Type:** Backend (BE)

## Objective

Protect state-changing API requests against CSRF attacks.

## Scope

- Configure CSRF protection using Spring Security.
- Implement `GET /api/v1/auth/csrf`.
- Allow unauthenticated clients to retrieve a CSRF token.
- Store CSRF tokens server-side using the HTTP session.
- Return the CSRF token for React to use in the `X-XSRF-TOKEN` header.
- Reject state-changing requests with missing or invalid CSRF tokens.
- Add automated tests.

## Acceptance Criteria

- Unauthenticated clients can retrieve a CSRF token.
- Valid CSRF tokens allow state-changing requests to proceed to normal authentication and validation.
- Missing or invalid CSRF tokens return HTTP 403.
- CSRF tokens are associated with the correct session.
- Automated tests pass.

## API Contract

### Request

`GET /api/v1/auth/csrf`

No request body. No authentication required.

### Success Response (200)

Headers:

```http
Content-Type: application/json
Set-Cookie: SESSION=<session-id>; HttpOnly; Secure; SameSite=Lax; Path=/
```

Body:

```json
{
  "token": "csrf-token-value",
  "headerName": "X-XSRF-TOKEN"
}
```

### Error Response (403)

Returned by state-changing endpoints when the CSRF token is missing or invalid.

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

- Registration and login APIs.
- Frontend CSRF token retrieval and integration.
- Authentication and authorization.
- Session restoration and logout.
