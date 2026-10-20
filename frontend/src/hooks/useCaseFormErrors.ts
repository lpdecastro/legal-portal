import type { FormInstance } from 'antd'
import type { CaseFormValues } from '../components/CaseForm'
import { useFormApiErrors } from './useFormApiErrors'

const caseFormFields: readonly (keyof CaseFormValues)[] = ['title', 'categoryId', 'description', 'location']

// Maps a failed case save onto the case form: API field errors go under their fields, anything else into
// `errorMessage`. Entered values are kept.
export function useCaseFormErrors(form: FormInstance<CaseFormValues>) {
  return useFormApiErrors(form, caseFormFields)
}
