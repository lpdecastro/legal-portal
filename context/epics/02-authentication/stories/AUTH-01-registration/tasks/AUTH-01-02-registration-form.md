# AUTH-01-02: Registration Form

**Status:** Not Started  
**Type:** Frontend (FE)

## Objective

Allow clients to register an account through the React UI.

## Scope

- Build registration form using Ant Design.
- Include first name, last name, email, password, and confirm password.
- Validate required fields, email format, password length (minimum 12 characters), and matching passwords.
- Integrate with `POST /api/v1/auth/register` using Fetch API.
- Follow the API contract defined in `AUTH-01-01-registration-api.md`.
- Retrieve a CSRF token using `GET /api/v1/auth/csrf` before submission.
- Follow the API contract in `AUTH-00-01-csrf-protection.md`.
- Include the token in the `X-XSRF-TOKEN` request header.
- Display validation errors and API errors.
- Show loading state during submission.
- Redirect to login after successful registration.
- Add automated tests.

## Acceptance Criteria

- Clients can register successfully through the form.
- Invalid inputs display appropriate validation messages.
- Duplicate emails display an error message.
- Submit button is disabled while submitting.
- Successful registration redirects to `/login`.
- Automated tests pass.

## Out of Scope

- Registration API implementation.
- Login and session management.
- Email verification.
