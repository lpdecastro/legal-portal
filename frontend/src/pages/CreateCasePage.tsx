import { useRef, useState } from 'react'
import { Alert, App, Button, Form, Input, Typography } from 'antd'
import { useNavigate } from 'react-router'
import type { CreateCaseRequest } from '../api/cases'
import { ApiError } from '../api/client'
import LegalCategorySelect from '../components/LegalCategorySelect'
import { useCreateCase } from '../hooks/useCreateCase'
import { caseDetailsPath } from '../routes/paths'

interface CreateCaseFormValues {
  title: string
  categoryId: string
  description?: string
  location?: string
}

type CaseField = keyof CreateCaseFormValues

const TITLE_MAX_LENGTH = 200
const LOCATION_MAX_LENGTH = 255
const GENERIC_ERROR_MESSAGE = 'Something went wrong. Please try again.'

const formFields: readonly CaseField[] = ['title', 'categoryId', 'description', 'location']

function isFormField(field: string): field is CaseField {
  return (formFields as readonly string[]).includes(field)
}

function CreateCasePage() {
  const [form] = Form.useForm<CreateCaseFormValues>()
  const { submit, submitting } = useCreateCase()
  const [errorMessage, setErrorMessage] = useState<string>()
  // Blocks a second save that starts before the `submitting` re-render disables the form.
  const savingRef = useRef(false)
  const navigate = useNavigate()
  const { message } = App.useApp()

  // Maps an API error onto form fields where possible, otherwise shows it in the alert. Entered values are kept.
  const showApiError = (error: unknown) => {
    if (!(error instanceof ApiError)) {
      setErrorMessage(GENERIC_ERROR_MESSAGE)
      return
    }
    const fieldErrors = error.problem?.fieldErrors ?? []
    const known = fieldErrors.flatMap(({ field, message: fieldMessage }) =>
      isFormField(field) ? [{ name: field, errors: [fieldMessage] }] : [],
    )
    form.setFields(known)
    if (known.length === 0 || known.length < fieldErrors.length) {
      setErrorMessage(error.message)
    }
  }

  const handleFinish = async ({ title, categoryId, description, location }: CreateCaseFormValues) => {
    if (savingRef.current) return
    savingRef.current = true
    setErrorMessage(undefined)
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

      {errorMessage && (
        <Alert type="error" showIcon title={errorMessage} style={{ marginBottom: 24, maxWidth: 720 }} />
      )}

      <Form<CreateCaseFormValues>
        form={form}
        layout="vertical"
        onFinish={handleFinish}
        disabled={submitting}
        style={{ maxWidth: 720 }}
      >
        <Form.Item
          label="Title"
          name="title"
          rules={[
            { required: true, whitespace: true, message: 'Title is required' },
            { max: TITLE_MAX_LENGTH, message: `Title must not exceed ${TITLE_MAX_LENGTH} characters` },
          ]}
        >
          <Input count={{ show: true, max: TITLE_MAX_LENGTH }} />
        </Form.Item>

        <Form.Item
          label="Legal category"
          name="categoryId"
          rules={[{ required: true, message: 'Legal category is required' }]}
        >
          <LegalCategorySelect />
        </Form.Item>

        <Form.Item label="Description" name="description">
          <Input.TextArea autoSize={{ minRows: 4, maxRows: 12 }} />
        </Form.Item>

        <Form.Item
          label="Location"
          name="location"
          rules={[{ max: LOCATION_MAX_LENGTH, message: `Location must not exceed ${LOCATION_MAX_LENGTH} characters` }]}
        >
          <Input count={{ show: true, max: LOCATION_MAX_LENGTH }} />
        </Form.Item>

        <Form.Item>
          <Button type="primary" htmlType="submit" loading={submitting}>
            Save Draft
          </Button>
        </Form.Item>
      </Form>
    </>
  )
}

export default CreateCasePage
