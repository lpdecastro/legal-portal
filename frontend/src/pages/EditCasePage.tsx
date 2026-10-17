import { useRef } from 'react'
import { Alert, App, Button, Flex, Form, Result, Spin, Typography } from 'antd'
import { useNavigate, useParams } from 'react-router'
import type { CaseStatus, LegalCase, UpdateCaseRequest } from '../api/cases'
import { ApiError } from '../api/client'
import CaseForm from '../components/CaseForm'
import type { CaseFormValues } from '../components/CaseForm'
import { useCase } from '../hooks/useCase'
import { useCaseFormErrors } from '../hooks/useCaseFormErrors'
import { useUpdateCase } from '../hooks/useUpdateCase'
import { caseDetailsPath, paths } from '../routes/paths'

const STATUS_LABELS: Record<CaseStatus, string> = {
  DRAFT: 'draft',
  SUBMITTED: 'submitted',
  CLOSED: 'closed',
}

// 404 covers other clients', deleted and nonexistent cases; 400 a malformed ID.
function isNotFound(error: unknown): boolean {
  return error instanceof ApiError && (error.status === 404 || error.status === 400)
}

// Only the fields that differ from the loaded case, normalized like Create Draft. A cleared optional field is sent as null.
function changedFields(legalCase: LegalCase, { title, categoryId, description, location }: CaseFormValues) {
  const next = {
    title: title.trim(),
    categoryId,
    description: description || null,
    location: location?.trim() || null,
  }
  const request: UpdateCaseRequest = {}
  if (next.title !== legalCase.title) request.title = next.title
  if (next.categoryId !== legalCase.categoryId) request.categoryId = next.categoryId
  if (next.description !== legalCase.description) request.description = next.description
  if (next.location !== legalCase.location) request.location = next.location
  return request
}

function EditDraftForm({ legalCase }: { legalCase: LegalCase }) {
  const [form] = Form.useForm<CaseFormValues>()
  const { submit, submitting } = useUpdateCase()
  const { errorMessage, clearError, showApiError } = useCaseFormErrors(form)
  // Blocks a second save that starts before the `submitting` re-render disables the form.
  const savingRef = useRef(false)
  const navigate = useNavigate()
  const { message } = App.useApp()

  const handleFinish = async (values: CaseFormValues) => {
    if (savingRef.current) return
    const request = changedFields(legalCase, values)
    if (Object.keys(request).length === 0) {
      message.info('No changes to save.')
      navigate(caseDetailsPath(legalCase.id))
      return
    }
    savingRef.current = true
    clearError()
    try {
      await submit(legalCase.id, request)
      message.success('Draft updated.')
      navigate(caseDetailsPath(legalCase.id))
    } catch (error) {
      showApiError(error)
    } finally {
      savingRef.current = false
    }
  }

  return (
    <CaseForm
      form={form}
      initialValues={{
        title: legalCase.title,
        categoryId: legalCase.categoryId,
        description: legalCase.description ?? undefined,
        location: legalCase.location ?? undefined,
      }}
      submitting={submitting}
      submitLabel="Save Changes"
      errorMessage={errorMessage}
      onFinish={handleFinish}
    />
  )
}

function EditCaseContent({ id }: { id: string }) {
  const { legalCase, loading, error, retry } = useCase(id)
  const navigate = useNavigate()

  if (loading) {
    return (
      <Flex justify="center" style={{ padding: 48 }}>
        <Spin size="large" description="Loading case…" />
      </Flex>
    )
  }

  if (isNotFound(error)) {
    return (
      <Result
        status="404"
        title="Case not found"
        subTitle="This case doesn't exist or you don't have access to it."
        extra={
          <Button type="primary" onClick={() => navigate(paths.cases)}>
            Back to My Cases
          </Button>
        }
      />
    )
  }

  if (!legalCase) {
    return (
      <Alert
        type="error"
        showIcon
        title="We couldn't load this case."
        description="Please check your connection and try again."
        action={
          <Button size="small" onClick={retry}>
            Retry
          </Button>
        }
        style={{ maxWidth: 720 }}
      />
    )
  }

  if (legalCase.status !== 'DRAFT') {
    return (
      <Result
        status="warning"
        title="This case can't be edited"
        subTitle={`Only drafts can be edited. This case is ${STATUS_LABELS[legalCase.status]}.`}
        extra={
          <Button type="primary" onClick={() => navigate(caseDetailsPath(legalCase.id))}>
            View case
          </Button>
        }
      />
    )
  }

  return <EditDraftForm legalCase={legalCase} />
}

function EditCasePage() {
  const { id = '' } = useParams()

  return (
    <>
      <Typography.Title level={2}>Edit Draft Case</Typography.Title>
      <Typography.Paragraph type="secondary">Update your draft. You can submit it once it's complete.</Typography.Paragraph>
      {/* Keyed so a different case starts with a fresh load and form. */}
      <EditCaseContent key={id} id={id} />
    </>
  )
}

export default EditCasePage
