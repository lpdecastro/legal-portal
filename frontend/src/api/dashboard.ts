import { getJson } from './client'
import type { CaseStatus } from './cases'

// The signed-in client's case counts; statuses without cases count as zero and `total` is their sum.
export interface CaseStatistics {
  total: number
  draft: number
  submitted: number
  closed: number
}

// One of the client's newest cases on the dashboard.
export interface RecentCase {
  id: string
  title: string
  status: CaseStatus
  createdAt: string
}

export interface DashboardSummary {
  statistics: CaseStatistics
  // The 5 newest cases, newest first.
  recentCases: RecentCase[]
}

// Loads the signed-in client's case counts and newest cases in one request.
export function fetchDashboardSummary(): Promise<DashboardSummary> {
  return getJson<DashboardSummary>('/dashboard/summary')
}
