import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderWithProviders } from '../test/render'
import ProfileMenu from './ProfileMenu'

describe('ProfileMenu', () => {
  it("shows the signed-in client's name", () => {
    renderWithProviders(<ProfileMenu />, {
      user: { id: 'u1', email: 'john@example.com', firstName: 'John', lastName: 'Doe' },
    })

    expect(screen.getByText('John Doe')).toBeInTheDocument()
  })

  it('falls back to a generic label when signed out', () => {
    renderWithProviders(<ProfileMenu />)

    expect(screen.getByText('My Account')).toBeInTheDocument()
  })
})
