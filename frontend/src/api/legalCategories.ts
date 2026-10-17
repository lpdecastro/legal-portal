import { getJson } from './client'

export interface LegalCategory {
  id: string
  name: string
  description: string | null
}

// Returns the active legal categories a case can be filed under.
export function fetchLegalCategories(): Promise<LegalCategory[]> {
  return getJson<LegalCategory[]>('/legal-categories')
}
