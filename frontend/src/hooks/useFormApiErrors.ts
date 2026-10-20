import { useCallback, useState } from 'react'
import type { FormInstance } from 'antd'
import { ApiError } from '../api/client'

interface UseFormApiErrorsResult {
  errorMessage?: string
  clearError: () => void
  showApiError: (error: unknown) => void
}

const GENERIC_ERROR_MESSAGE = 'Something went wrong. Please try again.'

// Maps a failed save onto a form: API field errors for `fields` go under those fields, anything else into
// `errorMessage`. Entered values are kept.
export function useFormApiErrors<Values>(
  form: FormInstance<Values>,
  fields: readonly (keyof Values & string)[],
): UseFormApiErrorsResult {
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
        (fields as readonly string[]).includes(field) ? [{ name: field, errors: [message] }] : [],
      )
      // antd types field names as deep paths; ours are top-level keys of `Values`.
      form.setFields(known as Parameters<FormInstance<Values>['setFields']>[0])
      if (known.length === 0 || known.length < fieldErrors.length) {
        setErrorMessage(error.message)
      }
    },
    [form, fields],
  )

  return { errorMessage, clearError, showApiError }
}
