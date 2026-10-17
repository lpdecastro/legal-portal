# Protected Routes

How the SPA keeps signed-out clients away from authenticated pages. Introduced in AUTH-03-02. It builds on [`session-restoration.md`](session-restoration.md).

## Route guard

- [`RequireAuth`](../frontend/src/auth/RequireAuth.tsx) is a layout route that wraps `AuthenticatedLayout` in [`routes/router.tsx`](../frontend/src/routes/router.tsx). Every page under it is protected: dashboard, cases (list, new, details, edit), profile and settings.
- If there is a signed-in client in auth state, it renders the page (`<Outlet />`). Otherwise it redirects to `/login` with `replace`, so Back doesn't return to the protected URL.
- `/` still redirects to `/dashboard`. A signed-out client is then sent on to `/login`.
- Public pages (login, register, forgot/reset password, verify email) and the 404 page are not guarded.

## Loading state

`RequireAuth` doesn't need its own loading state. `AuthProvider` shows only a full-screen `Spin` until `GET /auth/me` settles, so neither the guard nor any page renders before the session check is done. After a refresh, a valid session restores the client and they stay on the page they reloaded.

## Expired or invalid sessions

- **At start-up:** a 401 (or any failure) from `GET /auth/me` leaves the client signed out, and the guard redirects them to `/login`.
- **While signed in:** [`api/client.ts`](../frontend/src/api/client.ts) notifies `onUnauthorized` listeners whenever any API call returns 401. `AuthProvider` subscribes and clears the user, and the guard then redirects to `/login`. A 401 from a failed login is harmless because the client is already signed out.

## Tests

[`RequireAuth.test.tsx`](../frontend/src/auth/RequireAuth.test.tsx) renders the app's real route tree (`routes` with `useRoutes`) and covers:

- signed-in clients opening protected pages
- signed-out clients redirected from every protected path (and from `/`)
- a refresh with a valid session: only the loader shows until the client is restored on the same page
- a refresh with an expired or invalid session (401 from `/auth/me`): redirected to the login page
- a 401 from another API call while signed in: redirected to the login page
