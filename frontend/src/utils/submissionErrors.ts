import type { LegalCase } from '../api/cases'
import type { FieldError } from '../api/types'

const isBlank = (value: string | null) => value === null || value.trim() === ''

// The rules a draft must meet before it can be submitted; messages match the submit API's field errors.
export function submissionErrors(legalCase: LegalCase, categoryActive: boolean): FieldError[] {
  const errors: FieldError[] = []
  if (isBlank(legalCase.title)) {
    errors.push({ field: 'title', message: 'Title is required' })
  } else if (legalCase.title.length > 200) {
    errors.push({ field: 'title', message: 'Title must not exceed 200 characters' })
  }
  if (!categoryActive) {
    errors.push({ field: 'categoryId', message: 'Select an active legal category' })
  }
  if (isBlank(legalCase.description)) {
    errors.push({ field: 'description', message: 'Description is required' })
  }
  if (isBlank(legalCase.location)) {
    errors.push({ field: 'location', message: 'Location is required' })
  } else if (legalCase.location !== null && legalCase.location.length > 255) {
    errors.push({ field: 'location', message: 'Location must not exceed 255 characters' })
  }
  return errors
}
