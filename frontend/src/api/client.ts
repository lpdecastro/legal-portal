import type { CsrfTokenResponse, ProblemDetails } from './types'

export const API_BASE_URL = '/api/v1'

const GENERIC_ERROR_MESSAGE = 'Something went wrong. Please try again.'

// Thrown for any failed API call. Network failures have status 0 and no problem body.
export class ApiError extends Error {
  readonly status: number
  readonly problem?: ProblemDetails

  constructor(status: number, problem?: ProblemDetails) {
    super(problem?.detail ?? GENERIC_ERROR_MESSAGE)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }

  get errorCode(): string | undefined {
    return this.problem?.errorCode
  }
}

function isProblemDetails(value: unknown): value is ProblemDetails {
  return typeof value === 'object' && value !== null && 'status' in value
}

async function readProblem(response: Response): Promise<ProblemDetails | undefined> {
  try {
    const body: unknown = await response.json()
    return isProblemDetails(body) ? body : undefined
  } catch {
    return undefined
  }
}

async function send(path: string, init: RequestInit): Promise<Response> {
  let response: Response
  try {
    // Same origin: the browser sends the SESSION cookie the CSRF token is bound to.
    response = await fetch(`${API_BASE_URL}${path}`, { credentials: 'same-origin', ...init })
  } catch {
    throw new ApiError(0)
  }
  if (!response.ok) {
    throw new ApiError(response.status, await readProblem(response))
  }
  return response
}

export async function fetchCsrfToken(): Promise<CsrfTokenResponse> {
  const response = await send('/auth/csrf', { method: 'GET' })
  return (await response.json()) as CsrfTokenResponse
}

export async function getJson<TResponse>(path: string): Promise<TResponse> {
  const response = await send(path, {
    method: 'GET',
    headers: { Accept: 'application/json, application/problem+json' },
  })
  return (await response.json()) as TResponse
}

// Sends a CSRF-protected JSON request, fetching a fresh token first.
export async function postJson<TResponse>(path: string, body: unknown): Promise<TResponse> {
  const csrf = await fetchCsrfToken()
  const response = await send(path, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json, application/problem+json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(body),
  })
  return (await response.json()) as TResponse
}
