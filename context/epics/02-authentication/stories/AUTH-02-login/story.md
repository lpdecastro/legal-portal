# AUTH-02: Client Login

**Status:** Not Started

## User Story

As a registered client, I want to log in securely so I can access my dashboard.

## Acceptance Criteria

- Clients can log in using their email and password.
- Invalid credentials display a clear error message.
- Successful login creates an authenticated session.
- Clients are redirected to the dashboard after login.
- Login errors do not reveal whether an email exists.
- Repeated failed login attempts are rate-limited.

## Tasks

- AUTH-02-01: Login API (BE)
- AUTH-02-02: Login Form (FE)

## Definition of Done

- Both tasks are completed.
- Client login works end-to-end.
- Automated tests pass.
