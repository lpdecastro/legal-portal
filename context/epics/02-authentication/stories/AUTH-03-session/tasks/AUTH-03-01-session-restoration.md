# AUTH-03-01: Session Restoration

**Status:** Not Started  
**Type:** Backend (BE) + Frontend (FE)

## Objective

Restore the authenticated client's session after a page refresh.

## Scope

- Implement `GET /api/v1/auth/me` in Spring Boot.
- Return authenticated client information for valid sessions.
- Return HTTP 401 for missing, invalid, or expired sessions.
- Call `/auth/me` when React initializes.
- Restore authenticated user information in React state.
- Show loading state while checking the session.
- Handle expired or invalid sessions.
- Add automated tests.

## Acceptance Criteria

- Refreshing the page keeps clients logged in when their session is valid.
- Authenticated clients retain their user information after refresh.
- Missing, invalid, or expired sessions do not restore authentication.
- Session validation uses the existing session cookie.
- Automated tests pass.

## API Contract

### Request

`GET /api/v1/auth/me`

No request body. Browser sends the existing session cookie.

### Success Response (200)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
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
  "detail": "Authentication required.",
  "errorCode": "UNAUTHORIZED",
  "timestamp": "2026-10-15T02:17:00Z",
  "traceId": "a1b2c3d4"
}
```

Error responses use `Content-Type: application/problem+json`.

## Out of Scope

- Login and session creation.
- Protected route redirects.
- Logout.
