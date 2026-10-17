import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse, delay } from 'msw'
import { Button, Form } from 'antd'
import { describe, expect, it, vi } from 'vitest'
import { server } from '../test/mocks/server'
import { renderWithProviders } from '../test/render'
import LegalCategorySelect from './LegalCategorySelect'

const USER = { id: 'u1', email: 'john@example.com', firstName: 'John', lastName: 'Doe' }

// Deliberately out of order: the dropdown sorts by name.
const CATEGORIES = [
  { id: 'c-family', name: 'Family Law', description: 'Divorce, custody and support.' },
  { id: 'c-criminal', name: 'Criminal Law', description: null },
  { id: 'c-employment', name: 'Employment Law', description: 'Workplace disputes.' },
]

function mockCategories(resolver: Parameters<typeof http.get>[1]) {
  server.use(http.get('*/api/v1/legal-categories', resolver))
}

interface FormValues {
  categoryId?: string
}

function renderInForm(initialValues: FormValues = {}) {
  const onFinish = vi.fn<(values: FormValues) => void>()
  renderWithProviders(
    <Form<FormValues> initialValues={initialValues} onFinish={onFinish}>
      <Form.Item name="categoryId" label="Legal category">
        <LegalCategorySelect />
      </Form.Item>
      <Button htmlType="submit">Save</Button>
    </Form>,
    { user: USER },
  )
  return { onFinish }
}

const categorySelect = () => screen.getByRole('combobox', { name: 'Legal category' })

async function openDropdown() {
  const user = userEvent.setup()
  await waitFor(() => expect(categorySelect()).toBeEnabled())
  await user.click(categorySelect())
  return user
}

const optionLabels = () =>
  Array.from(document.querySelectorAll('.ant-select-item-option')).map((option) => option.textContent)

describe('LegalCategorySelect', () => {
  it('lists the categories alphabetically by name', async () => {
    mockCategories(() => HttpResponse.json(CATEGORIES))
    renderInForm()

    await openDropdown()

    await waitFor(() => expect(optionLabels()).toEqual(['Criminal Law', 'Employment Law', 'Family Law']))
  })

  it('puts the selected category ID in the form', async () => {
    mockCategories(() => HttpResponse.json(CATEGORIES))
    const { onFinish } = renderInForm()

    const user = await openDropdown()
    await user.click(await screen.findByTitle('Employment Law'))
    await user.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(onFinish).toHaveBeenCalledWith({ categoryId: 'c-employment' }))
  })

  it('preselects the existing category when editing a draft', async () => {
    mockCategories(() => HttpResponse.json(CATEGORIES))
    renderInForm({ categoryId: 'c-family' })

    const field = categorySelect().closest('.ant-select') as HTMLElement
    expect(await within(field).findByText('Family Law')).toBeInTheDocument()
  })

  it('shows a loading state while fetching the categories', async () => {
    mockCategories(async () => {
      await delay(50)
      return HttpResponse.json(CATEGORIES)
    })
    renderInForm({ categoryId: 'c-family' })

    expect(screen.getByText('Loading categories…')).toBeInTheDocument()
    expect(categorySelect()).toBeDisabled()
    // The raw ID of the preselected category never shows while loading.
    expect(screen.queryByText('c-family')).not.toBeInTheDocument()
    await waitFor(() => expect(categorySelect()).toBeEnabled())
  })

  it('shows an error and loads the categories again on retry', async () => {
    let requests = 0
    mockCategories(() => {
      requests += 1
      return requests === 1
        ? HttpResponse.json(
            { status: 500, errorCode: 'INTERNAL_ERROR', detail: 'Something went wrong.' },
            { status: 500, headers: { 'Content-Type': 'application/problem+json' } },
          )
        : HttpResponse.json(CATEGORIES)
    })
    renderInForm()
    const user = userEvent.setup()

    expect(await screen.findByText("We couldn't load the legal categories.")).toBeInTheDocument()
    expect(categorySelect()).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Retry' }))

    await waitFor(() => expect(screen.queryByText("We couldn't load the legal categories.")).not.toBeInTheDocument())
    await openDropdown()
    expect(await screen.findByTitle('Criminal Law')).toBeInTheDocument()
    expect(requests).toBe(2)
  })

  it('shows an error when the network is down', async () => {
    mockCategories(() => HttpResponse.error())
    renderInForm()

    expect(await screen.findByText("We couldn't load the legal categories.")).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument()
  })

  it('shows an empty state when no categories are available', async () => {
    mockCategories(() => HttpResponse.json([]))
    renderInForm()

    await openDropdown()

    expect(await screen.findByText('No legal categories available')).toBeInTheDocument()
  })
})
