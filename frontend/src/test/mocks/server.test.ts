import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from './server'

const categoriesUrl = new URL('/api/v1/legal-categories', window.location.origin).toString()

describe('MSW server', () => {
  it('returns the mocked response for a handled request', async () => {
    server.use(
      http.get('*/api/v1/legal-categories', () =>
        HttpResponse.json([{ id: 'c1', name: 'Family Law' }]),
      ),
    )

    const response = await fetch(categoriesUrl)

    expect(response.status).toBe(200)
    expect(await response.json()).toEqual([{ id: 'c1', name: 'Family Law' }])
  })

  it('returns a Problem Details error when a handler mocks one', async () => {
    server.use(
      http.get('*/api/v1/legal-categories', () =>
        HttpResponse.json(
          { status: 500, title: 'Internal Server Error', errorCode: 'INTERNAL_ERROR' },
          { status: 500, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )

    const response = await fetch(categoriesUrl)

    expect(response.status).toBe(500)
    expect(response.headers.get('Content-Type')).toBe('application/problem+json')
  })

  it('rejects requests that no handler covers', async () => {
    await expect(fetch(categoriesUrl)).rejects.toMatchObject({
      cause: expect.objectContaining({ message: expect.stringContaining('[MSW]') }),
    })
  })
})
