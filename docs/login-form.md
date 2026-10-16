# Login Form

How the `/login` page signs a client in and stores the user in React state. Introduced in AUTH-02-02. The backend side is described in [`session-login.md`](session-login.md) and CSRF handling in [`csrf-protection.md`](csrf-protection.md).

## Request flow

1. **Auth state is created at startup.** [`App`](../frontend/src/App.tsx) wraps `RouterProvider` in [`AuthProvider`](../frontend/src/auth/AuthProvider.tsx). The provider keeps `user` (`AuthenticatedUser | null`) in `useState` and passes a memoized `{ user, setUser }` through [`AuthContext`](../frontend/src/auth/AuthContext.ts).
2. **The page sets itself up.** [`LoginPage`](../frontend/src/pages/LoginPage.tsx) creates the Ant Design form with `Form.useForm<LoginRequest>()`. It also gets `{ submit, submitting }` from [`useLogin`](../frontend/src/hooks/useLogin.ts), keeps local `errorMessage` state and calls `useNavigate()`. `useLogin` gets `setUser` from [`useAuth`](../frontend/src/hooks/useAuth.ts), which throws if no `AuthProvider` is above it.
3. **Client-side validation runs.** The email field trims input with `normalize` and requires a value in email format. The password field is only required; length rules apply to registration only. If validation fails, Ant Design shows the messages under the fields and does not call `onFinish`.
4. **`handleFinish` submits the form.** It clears the alert with `setErrorMessage(undefined)` and calls `submit({ email: email.trim(), password })`.
5. **`useLogin.submit` calls the API.** It sets `submitting` to true, which disables the form (`disabled={submitting}`) and shows a spinner on the button (`loading={submitting}`). It then calls `login(request)`. A `finally` block resets `submitting` whether the call succeeds or fails.
6. **The client fetches a CSRF token, then posts.** `login` in [`api/auth.ts`](../frontend/src/api/auth.ts) calls `postJson<AuthenticatedUser>('/auth/login', request)`. `postJson` in [`api/client.ts`](../frontend/src/api/client.ts) works in two steps:
   - `fetchCsrfToken()` calls `GET /api/v1/auth/csrf` and returns `{ token, headerName }`.
   - `POST /api/v1/auth/login` is sent with a JSON body and the token in the `X-XSRF-TOKEN` header.

   Both requests go through `send`, which uses `credentials: 'same-origin'` so the browser attaches the `SESSION` cookie. On a non-2xx response, `send` throws an `ApiError` that holds the status and the parsed Problem Details. On a network failure, it throws `ApiError(0)`.
7. **Success.** The backend returns 200 with `{ id, email, firstName, lastName }` and rotates the session ID and CSRF token. `submit` calls `setUser(user)`. `handleFinish` then calls `navigate(paths.dashboard, { replace: true })`, so Back does not return to the login page.
8. **Errors.** `submit` has no `catch`, so the error goes back to `handleFinish`. `handleFinish` calls `showApiError` and returns without redirecting.

## Error handling

`showApiError` in `LoginPage` maps errors as follows:

| Error | Shown as |
| --- | --- |
| Not an `ApiError` | Alert: "Something went wrong. Please try again." |
| 401 `UNAUTHORIZED` | Alert: "Invalid email or password." Always the same, so the page never reveals whether an email is registered |
| 400 `VALIDATION_ERROR` | `fieldErrors` for `email`/`password` (checked by `isFormField`) go under the inputs via `form.setFields`. If any field errors don't match a form field, the server's `detail` also shows in the alert |
| 429 `RATE_LIMIT_EXCEEDED`, others | Alert with the server's `detail` (`error.message`) |

## State lifetime

React keeps only an in-memory copy of the user. The real session is the HttpOnly `SESSION` cookie. A page refresh clears `user` but keeps the session. Restoring the user through `GET /api/v1/auth/me` is planned for a later story.

## Testing

[`renderWithProviders`](../frontend/src/test/render.tsx) wraps components in `AuthProvider`, so `useAuth`/`useLogin` work in tests. [`LoginPage.test.tsx`](../frontend/src/pages/LoginPage.test.tsx) mocks `/auth/csrf` and `/auth/login` with MSW `server.use(...)` and covers the success and error paths above.
