import { Navigate, Outlet } from 'react-router'
import { useAuth } from '../hooks/useAuth'
import { paths } from '../routes/paths'

// Layout route for pages that need a signed-in client. AuthProvider renders nothing but its loading state until the
// session check settles, so by the time this renders `user` is final: either a restored client or signed out.
function RequireAuth() {
  const { user } = useAuth()

  if (!user) {
    return <Navigate to={paths.login} replace />
  }

  return <Outlet />
}

export default RequireAuth
