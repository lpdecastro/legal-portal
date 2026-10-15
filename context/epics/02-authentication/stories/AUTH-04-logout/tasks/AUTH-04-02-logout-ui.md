# AUTH-04-02: Logout UI

**Status:** Not Started  
**Type:** Frontend (FE)

## Objective

Allow authenticated clients to log out through the React UI.

## Scope

- Add logout action to the header profile menu.
- Integrate with `POST /api/v1/auth/logout` using Fetch API.
- Follow the API contract in `AUTH-04-01-logout-api.md`.
- Include the CSRF token in the request header.
- Clear frontend authentication state after successful logout.
- Redirect to `/login`.
- Handle API errors and loading state.
- Add automated tests.

## Acceptance Criteria

- Clients can log out using the profile menu.
- Successful logout redirects to `/login`.
- Authenticated user information is cleared from React state.
- Protected pages are inaccessible after logout.
- Logout errors display an appropriate message without falsely marking the client as logged out.
- Automated tests pass.

## Out of Scope

- Logout API implementation.
- Server-side session invalidation.
- Protected route implementation.
