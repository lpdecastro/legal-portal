import { test as base, expect } from '@playwright/test'

type Fixtures = {
  failOnPageErrors: void
}

export const test = base.extend<Fixtures>({
  // Fails a test whose page throws an uncaught error, even if its assertions passed
  failOnPageErrors: [
    async ({ page }, use) => {
      const errors: Error[] = []
      page.on('pageerror', (error) => errors.push(error))
      await use()
      expect(errors, 'uncaught errors in the page').toEqual([])
    },
    { auto: true },
  ],
})

export { expect }
