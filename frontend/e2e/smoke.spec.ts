import { expect, test } from './fixtures'

test('API is reachable through the app and connected to the database', async ({ request }) => {
  // The CSRF token is stored in a Spring Session row, so a 200 means the backend reached PostgreSQL
  const response = await request.get('/api/v1/auth/csrf')

  expect(response.status()).toBe(200)
  expect(await response.json()).toMatchObject({ token: expect.any(String) })
})

test('app loads and sends a signed-out visitor to login', async ({ page }) => {
  await page.goto('/')

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('heading', { name: 'Log in' })).toBeVisible()
})
