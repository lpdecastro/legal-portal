import { ApiError, getJson, patchJson, postJson } from './client'

export type CaseStatus = 'DRAFT' | 'SUBMITTED' | 'CLOSED'

export interface CreateCaseRequest {
  title: string
  categoryId: string
  description?: string
  location?: string
}

// Partial draft update: omitted fields stay unchanged, `null` clears an optional field.
export interface UpdateCaseRequest {
  title?: string
  categoryId?: string
  description?: string | null
  location?: string | null
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

// One row of the client's case list; `category` is the category name.
export interface CaseSummary {
  id: string
  title: string
  category: string
  status: CaseStatus
  createdAt: string
}

export interface CaseListResponse {
  content: CaseSummary[]
}

const casePath = (id: string): string => `/cases/${encodeURIComponent(id)}`

// Creates a new draft case owned by the signed-in client (201).
export function createCase(request: CreateCaseRequest): Promise<LegalCase> {
  return postJson<LegalCase>('/cases', request)
}

// Lists the signed-in client's cases in every status, newest first.
export async function fetchCases(): Promise<CaseSummary[]> {
  const { content } = await getJson<CaseListResponse>('/cases')
  return content
}

// Loads one of the signed-in client's cases; other clients' and missing cases answer 404 CASE_NOT_FOUND.
export function fetchCase(id: string): Promise<LegalCase> {
  return getJson<LegalCase>(casePath(id))
}

// Partially updates one of the signed-in client's drafts; non-drafts answer 409 INVALID_CASE_STATE.
export function updateCase(id: string, request: UpdateCaseRequest): Promise<LegalCase> {
  return patchJson<LegalCase>(casePath(id), request)
}

// True when a case load failed because the case is missing or inaccessible: 404 covers other clients', deleted and
// nonexistent cases; 400 a malformed ID.
export function isCaseNotFound(error: unknown): boolean {
  return error instanceof ApiError && (error.status === 404 || error.status === 400)
}
