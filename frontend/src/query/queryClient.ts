import { QueryClient } from '@tanstack/react-query'

// Failed queries show an error with a Retry button straight away instead of retrying in the background.
export function createQueryClient(): QueryClient {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } })
}
