# AUTH-02-02: Login Form

**Status:** Not Started  
**Type:** Frontend (FE)

## Objective

Allow registered clients to log in through the React UI.

## Scope

- Build login form using Ant Design.
- Include email and password fields.
- Validate required fields and email format.
- Integrate with `POST /api/v1/auth/login` using Fetch API.
- Follow the API contract defined in `AUTH-02-01-login-api.md`.
- Handle session cookies and CSRF tokens.
- Retrieve a CSRF token using `GET /api/v1/auth/csrf` before submission.
- Follow the API contract in `AUTH-00-01-csrf-protection.md`.
- Include the token in the `X-XSRF-TOKEN` request header.
- Store authenticated user information in React state.
- Display validation and API errors.
- Show loading state during submission.
- Redirect to dashboard after successful login.
- Add automated tests.

## Acceptance Criteria

- Clients can log in with valid credentials.
- Invalid inputs display validation messages.
- Invalid credentials display a generic error message.
- Submit button is disabled while submitting.
- Successful login redirects to `/dashboard`.
- Automated tests pass.

## Out of Scope

- Login API implementation.
- Session restoration after page refresh.
- Protected routes.
- Logout.
