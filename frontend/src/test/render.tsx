import type { ReactElement } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { RenderOptions, RenderResult } from '@testing-library/react'
import { App as AntApp, ConfigProvider } from 'antd'
import { MemoryRouter } from 'react-router'
import type { AuthenticatedUser } from '../api/auth'
import AuthProvider from '../auth/AuthProvider'
import { createQueryClient } from '../query/queryClient'
import { theme } from '../theme/theme'

// jsdom never fires the transition events antd's animations wait for, so closed dialogs would never hide.
const testTheme = { ...theme, token: { ...theme.token, motion: false } }

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
  // A fresh cache per test, so no test sees another's responses.
  const queryClient = createQueryClient()
  return render(ui, {
    wrapper: ({ children }) => (
      <ConfigProvider theme={testTheme}>
        <AntApp>
          <QueryClientProvider client={queryClient}>
            <AuthProvider initialUser={user}>
              <MemoryRouter initialEntries={initialEntries}>{children}</MemoryRouter>
            </AuthProvider>
          </QueryClientProvider>
        </AntApp>
      </ConfigProvider>
    ),
    ...options,
  })
}
