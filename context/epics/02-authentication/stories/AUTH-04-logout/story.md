# AUTH-04: Client Logout

**Status:** Not Started

## User Story

As a logged-in client, I want to log out securely so others cannot access my account using the same session.

## Acceptance Criteria

- Clients can log out using the logout action.
- Logging out invalidates the server-side session.
- Clients are redirected to `/login` after logout.
- Logged-out clients cannot access protected pages.
- Previously authenticated API requests return HTTP 401 after logout.

## Tasks

- AUTH-04-01: Logout API (BE)
- AUTH-04-02: Logout UI (FE)

## Definition of Done

- Both tasks are completed.
- Client logout works end-to-end.
- Automated tests pass.
