export interface FieldError {
  field: string
  message: string
}

// RFC 9457 Problem Details with the API's custom extensions.
export interface ProblemDetails {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  errorCode?: string
  timestamp?: string
  traceId?: string
  fieldErrors?: FieldError[]
}

export interface CsrfTokenResponse {
  token: string
  headerName: string
}
