import { useRef } from 'react'
import { App, Form, Typography } from 'antd'
import { useNavigate } from 'react-router'
import type { CreateCaseRequest } from '../api/cases'
import CaseForm from '../components/CaseForm'
import type { CaseFormValues } from '../components/CaseForm'
import { useCaseFormErrors } from '../hooks/useCaseFormErrors'
import { useCreateCase } from '../hooks/useCreateCase'
import { caseDetailsPath } from '../routes/paths'

function CreateCasePage() {
  const [form] = Form.useForm<CaseFormValues>()
  const { submit, submitting } = useCreateCase()
  const { errorMessage, clearError, showApiError } = useCaseFormErrors(form)
  // Blocks a second save that starts before the `submitting` re-render disables the form.
  const savingRef = useRef(false)
  const navigate = useNavigate()
  const { message } = App.useApp()

  const handleFinish = async ({ title, categoryId, description, location }: CaseFormValues) => {
    if (savingRef.current) return
    savingRef.current = true
    clearError()
    const request: CreateCaseRequest = {
      title: title.trim(),
      categoryId,
      description: description || undefined,
      location: location?.trim() || undefined,
    }
    try {
      const created = await submit(request)
      message.success('Draft saved.')
      navigate(caseDetailsPath(created.id))
    } catch (error) {
      showApiError(error)
    } finally {
      savingRef.current = false
    }
  }

  return (
    <>
      <Typography.Title level={2}>Create Draft Case</Typography.Title>
      <Typography.Paragraph type="secondary">
        Save your case as a draft. You can complete and submit it later.
      </Typography.Paragraph>

      <CaseForm
        form={form}
        submitting={submitting}
        submitLabel="Save Draft"
        errorMessage={errorMessage}
        onFinish={handleFinish}
      />
    </>
  )
}

export default CreateCasePage
