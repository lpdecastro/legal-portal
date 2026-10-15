import type { ReactElement } from 'react'
import { render } from '@testing-library/react'
import type { RenderOptions, RenderResult } from '@testing-library/react'
import { App as AntApp, ConfigProvider } from 'antd'
import { MemoryRouter } from 'react-router'
import { theme } from '../theme/theme'

interface RenderWithProvidersOptions extends Omit<RenderOptions, 'wrapper'> {
  initialEntries?: string[]
}

// Renders a component inside the same providers as the app, with an in-memory router.
export function renderWithProviders(
  ui: ReactElement,
  { initialEntries = ['/'], ...options }: RenderWithProvidersOptions = {},
): RenderResult {
  return render(ui, {
    wrapper: ({ children }) => (
      <ConfigProvider theme={theme}>
        <AntApp>
          <MemoryRouter initialEntries={initialEntries}>{children}</MemoryRouter>
        </AntApp>
      </ConfigProvider>
    ),
    ...options,
  })
}
