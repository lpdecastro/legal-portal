import { createBrowserRouter, Navigate } from 'react-router'
import type { RouteObject } from 'react-router'
import RequireAuth from '../auth/RequireAuth'
import AuthenticatedLayout from '../layouts/AuthenticatedLayout'
import PublicLayout from '../layouts/PublicLayout'
import CaseDetailsPage from '../pages/CaseDetailsPage'
import CreateCasePage from '../pages/CreateCasePage'
import DashboardPage from '../pages/DashboardPage'
import EditCasePage from '../pages/EditCasePage'
import ForgotPasswordPage from '../pages/ForgotPasswordPage'
import LoginPage from '../pages/LoginPage'
import MyCasesPage from '../pages/MyCasesPage'
import NotFoundPage from '../pages/NotFoundPage'
import ProfilePage from '../pages/ProfilePage'
import RegisterPage from '../pages/RegisterPage'
import ResetPasswordPage from '../pages/ResetPasswordPage'
import SettingsPage from '../pages/SettingsPage'
import VerifyEmailPage from '../pages/VerifyEmailPage'
import { paths } from './paths'

export const routes: RouteObject[] = [
  // Signed-out clients are sent on from /dashboard to /login by RequireAuth.
  { path: '/', element: <Navigate to={paths.dashboard} replace /> },
  {
    element: <PublicLayout />,
    children: [
      { path: paths.login, element: <LoginPage /> },
      { path: paths.register, element: <RegisterPage /> },
      { path: paths.forgotPassword, element: <ForgotPasswordPage /> },
      { path: paths.resetPassword, element: <ResetPasswordPage /> },
      { path: paths.verifyEmail, element: <VerifyEmailPage /> },
    ],
  },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AuthenticatedLayout />,
        children: [
          { path: paths.dashboard, element: <DashboardPage /> },
          { path: paths.cases, element: <MyCasesPage /> },
          { path: paths.createCase, element: <CreateCasePage /> },
          { path: paths.caseDetails, element: <CaseDetailsPage /> },
          { path: paths.editCase, element: <EditCasePage /> },
          { path: paths.profile, element: <ProfilePage /> },
          { path: paths.settings, element: <SettingsPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
]

export const router = createBrowserRouter(routes)
