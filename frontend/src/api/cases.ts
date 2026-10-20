import { ApiError, deleteNoContent, getJson, patchJson, postForJson, postJson } from './client'

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
  // Null until the case is submitted.
  submittedAt: string | null
  // Null until the case is closed.
  closedAt: string | null
}

// One row of the client's case list; `category` is the category name.
export interface CaseSummary {
  id: string
  title: string
  category: string
  status: CaseStatus
  createdAt: string
}

// Zero-based page number and page size, as the cases API expects.
export interface PageRequest {
  page: number
  size: number
}

// A page of cases, optionally limited to one status; no status lists every status.
export interface CaseListRequest extends PageRequest {
  status?: CaseStatus
}

// Where a page sits in the full list; `number` is zero-based.
export interface PageMetadata {
  size: number
  number: number
  totalElements: number
  totalPages: number
}

export interface CaseListResponse {
  content: CaseSummary[]
  page: PageMetadata
}

const casePath = (id: string): string => `/cases/${encodeURIComponent(id)}`

// Creates a new draft case owned by the signed-in client (201).
export function createCase(request: CreateCaseRequest): Promise<LegalCase> {
  return postJson<LegalCase>('/cases', request)
}

// Lists one page of the signed-in client's cases, newest first, in every status unless one is given.
export function fetchCases({ page, size, status }: CaseListRequest): Promise<CaseListResponse> {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  if (status) query.set('status', status)
  return getJson<CaseListResponse>(`/cases?${query}`)
}

// Loads one of the signed-in client's cases; other clients' and missing cases answer 404 CASE_NOT_FOUND.
export function fetchCase(id: string): Promise<LegalCase> {
  return getJson<LegalCase>(casePath(id))
}

// Partially updates one of the signed-in client's drafts; non-drafts answer 409 INVALID_CASE_STATE.
export function updateCase(id: string, request: UpdateCaseRequest): Promise<LegalCase> {
  return patchJson<LegalCase>(casePath(id), request)
}

// Soft-deletes one of the signed-in client's drafts (204); non-drafts answer 409 INVALID_CASE_STATE, other clients',
// deleted and missing cases 404 CASE_NOT_FOUND.
export function deleteCase(id: string): Promise<void> {
  return deleteNoContent(casePath(id))
}

// Submits one of the signed-in client's drafts and returns it as SUBMITTED; non-drafts answer 409 INVALID_CASE_STATE,
// incomplete drafts 400 VALIDATION_ERROR with every failing field.
export function submitCase(id: string): Promise<LegalCase> {
  return postForJson<LegalCase>(`${casePath(id)}/submit`)
}

// Closes one of the signed-in client's submitted cases and returns it as CLOSED; drafts and closed cases answer 409
// INVALID_CASE_STATE.
export function closeCase(id: string): Promise<LegalCase> {
  return postForJson<LegalCase>(`${casePath(id)}/close`)
}

// True when a case load failed because the case is missing or inaccessible: 404 covers other clients', deleted and
// nonexistent cases; 400 a malformed ID.
export function isCaseNotFound(error: unknown): boolean {
  return error instanceof ApiError && (error.status === 404 || error.status === 400)
}
