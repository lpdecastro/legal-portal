import { useCallback, useState } from 'react'
import type { FormInstance } from 'antd'
import { ApiError } from '../api/client'
import type { CaseFormValues } from '../components/CaseForm'

type CaseField = keyof CaseFormValues

interface UseCaseFormErrorsResult {
  errorMessage?: string
  clearError: () => void
  showApiError: (error: unknown) => void
}

const GENERIC_ERROR_MESSAGE = 'Something went wrong. Please try again.'

const formFields: readonly CaseField[] = ['title', 'categoryId', 'description', 'location']

function isFormField(field: string): field is CaseField {
  return (formFields as readonly string[]).includes(field)
}

// Maps a failed save onto the case form: API field errors go under their fields, anything else into `errorMessage`.
// Entered values are kept.
export function useCaseFormErrors(form: FormInstance<CaseFormValues>): UseCaseFormErrorsResult {
  const [errorMessage, setErrorMessage] = useState<string>()

  const clearError = useCallback(() => setErrorMessage(undefined), [])

  const showApiError = useCallback(
    (error: unknown) => {
      if (!(error instanceof ApiError)) {
        setErrorMessage(GENERIC_ERROR_MESSAGE)
        return
      }
      const fieldErrors = error.problem?.fieldErrors ?? []
      const known = fieldErrors.flatMap(({ field, message }) =>
        isFormField(field) ? [{ name: field, errors: [message] }] : [],
      )
      form.setFields(known)
      if (known.length === 0 || known.length < fieldErrors.length) {
        setErrorMessage(error.message)
      }
    },
    [form],
  )

  return { errorMessage, clearError, showApiError }
}
