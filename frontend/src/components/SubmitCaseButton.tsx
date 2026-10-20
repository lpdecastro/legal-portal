import { App, Button, Modal, Typography } from 'antd'
import { SendOutlined } from '@ant-design/icons'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import type { LegalCase } from '../api/cases'
import type { FieldError } from '../api/types'
import { useSubmitCase } from '../hooks/useSubmitCase'
import { editCasePath } from '../routes/paths'
import { submissionErrors } from '../utils/submissionErrors'

interface SubmitCaseButtonProps {
  legalCase: LegalCase
  // False when the case's category is inactive; undefined when the categories couldn't be loaded (the API checks).
  categoryActive?: boolean
  // Called with the case as returned by the API after a successful submission.
  onSubmitted: (legalCase: LegalCase) => void
  // Called when the shown case is out of date (already submitted, closed, deleted or inaccessible).
  onStale: () => void
}

type Dialog = { kind: 'confirm' } | { kind: 'incomplete'; errors: FieldError[] }

function submitErrorMessage(status: number): string {
  if (status === 404) return 'This case no longer exists or you no longer have access to it.'
  if (status === 409) return 'This case has already been submitted or closed.'
  return "We couldn't submit this case. Please try again."
}

function apiFieldErrors(error: ApiError): FieldError[] {
  const fieldErrors = error.problem?.fieldErrors ?? []
  return fieldErrors.length > 0 ? fieldErrors : [{ field: '', message: error.message }]
}

// Submits a draft after checking it's complete and asking for confirmation. Incomplete drafts (found here or by the
// API) get a dialog listing what's missing with a way to Edit Draft.
function SubmitCaseButton({ legalCase, categoryActive, onSubmitted, onStale }: SubmitCaseButtonProps) {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const { submit, submitting } = useSubmitCase()
  const [dialog, setDialog] = useState<Dialog>()

  const handleClick = () => {
    const errors = submissionErrors(legalCase, categoryActive ?? true)
    setDialog(errors.length > 0 ? { kind: 'incomplete', errors } : { kind: 'confirm' })
  }

  const handleSubmit = async () => {
    try {
      const submitted = await submit(legalCase.id)
      if (!submitted) return
      message.success('Case submitted.')
      onSubmitted(submitted)
    } catch (error) {
      if (error instanceof ApiError && error.status === 400) {
        setDialog({ kind: 'incomplete', errors: apiFieldErrors(error) })
        return
      }
      setDialog(undefined)
      const status = error instanceof ApiError ? error.status : 0
      // 401: the session ended and the client is being sent to the login page.
      if (status === 401) return
      message.error(submitErrorMessage(status))
      if (status === 404 || status === 409) onStale()
    }
  }

  const close = () => setDialog(undefined)

  return (
    <>
      <Button icon={<SendOutlined />} loading={submitting} onClick={handleClick}>
        Submit
      </Button>
      {/* One dialog for both steps: a 400 from the API turns the confirmation into the incomplete list. */}
      <Modal
        open={dialog !== undefined}
        {...(dialog?.kind === 'incomplete'
          ? {
              title: "This draft isn't ready to submit",
              okText: 'Edit Draft',
              cancelText: 'Close',
              onOk: () => navigate(editCasePath(legalCase.id)),
            }
          : {
              title: 'Submit this case?',
              okText: 'Submit',
              confirmLoading: submitting,
              onOk: handleSubmit,
            })}
        cancelButtonProps={{ disabled: submitting }}
        closable={!submitting}
        mask={{ closable: !submitting }}
        keyboard={!submitting}
        onCancel={close}
      >
        {dialog?.kind === 'incomplete' ? (
          <>
            <Typography.Paragraph>Complete the following before submitting:</Typography.Paragraph>
            <ul>
              {dialog.errors.map(({ field, message: text }) => (
                <li key={`${field}:${text}`}>{text}</li>
              ))}
            </ul>
          </>
        ) : (
          'Once submitted, this case can no longer be edited or deleted.'
        )}
      </Modal>
    </>
  )
}

export default SubmitCaseButton
