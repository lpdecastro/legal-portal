import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { caseDetailsPath, editCasePath, paths } from '../routes/paths'

interface PlaceholderProps {
  title: string
  children?: ReactNode
}

const navLinks = [
  { to: paths.login, label: 'Login' },
  { to: paths.register, label: 'Register' },
  { to: paths.forgotPassword, label: 'Forgot Password' },
  { to: paths.resetPassword, label: 'Reset Password' },
  { to: paths.verifyEmail, label: 'Verify Email' },
  { to: paths.dashboard, label: 'Dashboard' },
  { to: paths.cases, label: 'My Cases' },
  { to: paths.createCase, label: 'Create Case' },
  { to: caseDetailsPath('example'), label: 'Case Details' },
  { to: editCasePath('example'), label: 'Edit Draft' },
  { to: paths.profile, label: 'Profile' },
  { to: paths.settings, label: 'Settings' },
]

function Placeholder({ title, children }: PlaceholderProps) {
  return (
    <main>
      <nav>
        <ul>
          {navLinks.map(({ to, label }) => (
            <li key={to}>
              <Link to={to}>{label}</Link>
            </li>
          ))}
        </ul>
      </nav>
      <h1>{title}</h1>
      {children}
    </main>
  )
}

export default Placeholder
