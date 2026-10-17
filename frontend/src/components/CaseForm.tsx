import { Alert, Button, Form, Input } from 'antd'
import type { FormInstance } from 'antd'
import LegalCategorySelect from './LegalCategorySelect'

export interface CaseFormValues {
  title: string
  categoryId: string
  description?: string
  location?: string
}

interface CaseFormProps {
  form: FormInstance<CaseFormValues>
  initialValues?: Partial<CaseFormValues>
  submitting: boolean
  submitLabel: string
  // Shown in an alert above the fields, e.g. an API failure that doesn't map onto a field.
  errorMessage?: string
  onFinish: (values: CaseFormValues) => void
}

const TITLE_MAX_LENGTH = 200
const LOCATION_MAX_LENGTH = 255

// Draft case fields shared by the Create Draft and Edit Draft pages. Disabled while `submitting`.
function CaseForm({ form, initialValues, submitting, submitLabel, errorMessage, onFinish }: CaseFormProps) {
  return (
    <>
      {errorMessage && (
        <Alert type="error" showIcon title={errorMessage} style={{ marginBottom: 24, maxWidth: 720 }} />
      )}

      <Form<CaseFormValues>
        form={form}
        layout="vertical"
        initialValues={initialValues}
        onFinish={onFinish}
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
            {submitLabel}
          </Button>
        </Form.Item>
      </Form>
    </>
  )
}

export default CaseForm
