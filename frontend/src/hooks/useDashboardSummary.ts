import { useQuery } from '@tanstack/react-query'
import { fetchDashboardSummary } from '../api/dashboard'

export const dashboardSummaryQueryKey = ['dashboard', 'summary'] as const

// The signed-in client's dashboard summary. Every component on the dashboard reads the same query, so the page makes
// a single request; returning to the dashboard shows the cached summary while it refreshes.
export function useDashboardSummary() {
  return useQuery({ queryKey: dashboardSummaryQueryKey, queryFn: fetchDashboardSummary })
}
