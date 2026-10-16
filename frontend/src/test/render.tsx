import type { ReactElement } from 'react'
import { render } from '@testing-library/react'
import type { RenderOptions, RenderResult } from '@testing-library/react'
import { App as AntApp, ConfigProvider } from 'antd'
import { MemoryRouter } from 'react-router'
import type { AuthenticatedUser } from '../api/auth'
import AuthProvider from '../auth/AuthProvider'
import { theme } from '../theme/theme'

interface RenderWithProvidersOptions extends Omit<RenderOptions, 'wrapper'> {
  initialEntries?: string[]
  // The signed-in client; session restoration (GET /auth/me) is skipped. Defaults to signed out.
  user?: AuthenticatedUser | null
}

// Renders a component inside the same providers as the app, with an in-memory router.
export function renderWithProviders(
  ui: ReactElement,
  { initialEntries = ['/'], user = null, ...options }: RenderWithProvidersOptions = {},
): RenderResult {
  return render(ui, {
    wrapper: ({ children }) => (
      <ConfigProvider theme={theme}>
        <AntApp>
          <AuthProvider initialUser={user}>
            <MemoryRouter initialEntries={initialEntries}>{children}</MemoryRouter>
          </AuthProvider>
        </AntApp>
      </ConfigProvider>
    ),
    ...options,
  })
}
