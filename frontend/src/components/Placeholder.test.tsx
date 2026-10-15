import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderWithProviders } from '../test/render'
import Placeholder from './Placeholder'

describe('Placeholder', () => {
  it('renders the title as a heading with the coming-soon message', () => {
    renderWithProviders(<Placeholder title="My Cases" />)

    expect(screen.getByRole('heading', { level: 2, name: 'My Cases' })).toBeInTheDocument()
    expect(screen.getByText('This page is coming soon.')).toBeInTheDocument()
  })

  it('renders children below the message', () => {
    renderWithProviders(
      <Placeholder title="Case Details">
        <p>Case ID: 42</p>
      </Placeholder>,
    )

    expect(screen.getByText('Case ID: 42')).toBeInTheDocument()
  })
})
