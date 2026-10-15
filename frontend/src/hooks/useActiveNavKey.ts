import { matchPath, useLocation } from 'react-router'
import { paths } from '../routes/paths'

export type NavKey = 'dashboard' | 'cases' | 'createCase' | 'profile' | 'settings'

// Ordered most-specific first so `/cases/new` wins over `/cases/:id`.
const navMatchers: ReadonlyArray<{ key: NavKey; patterns: readonly string[] }> = [
  { key: 'createCase', patterns: [paths.createCase] },
  { key: 'cases', patterns: [paths.cases, paths.caseDetails, paths.editCase] },
  { key: 'dashboard', patterns: [paths.dashboard] },
  { key: 'profile', patterns: [paths.profile] },
  { key: 'settings', patterns: [paths.settings] },
]

export function useActiveNavKey(): NavKey | undefined {
  const { pathname } = useLocation()
  return navMatchers.find(({ patterns }) =>
    patterns.some((pattern) => matchPath(pattern, pathname)),
  )?.key
}
