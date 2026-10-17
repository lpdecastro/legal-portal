import { postJson } from './client'

export type CaseStatus = 'DRAFT' | 'SUBMITTED' | 'CLOSED'

export interface CreateCaseRequest {
  title: string
  categoryId: string
  description?: string
  location?: string
}

// A client's case, as returned by the cases API.
export interface LegalCase {
  id: string
  title: string
  categoryId: string
  description: string | null
  location: string | null
  status: CaseStatus
  createdAt: string
  updatedAt: string
}

// Creates a new draft case owned by the signed-in client (201).
export function createCase(request: CreateCaseRequest): Promise<LegalCase> {
  return postJson<LegalCase>('/cases', request)
}
