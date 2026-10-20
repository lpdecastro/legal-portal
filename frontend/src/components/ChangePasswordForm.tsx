import { Alert, App, Button, Form, Input } from 'antd'
import type { ChangePasswordRequest } from '../api/profile'
import { ApiError } from '../api/client'
import { useAuth } from '../hooks/useAuth'
import { useChangePassword } from '../hooks/useChangePassword'
import { useFormApiErrors } from '../hooks/useFormApiErrors'
import { confirmPasswordRules, newPasswordRules, PASSWORD_MIN_LENGTH } from '../utils/passwordRules'

const changePasswordFields: readonly (keyof ChangePasswordRequest)[] = [
  'currentPassword',
  'newPassword',
  'confirmPassword',
]

const newPasswordFieldRules = newPasswordRules('New password is required')
const confirmNewPasswordRules = confirmPasswordRules('newPassword', 'Please confirm your new password')

// Changes the client's password. The fields are cleared after a successful change; on errors the entered values
// are kept. Passwords are sent as entered, never trimmed, and never stored outside the form.
function ChangePasswordForm() {
  const [form] = Form.useForm<ChangePasswordRequest>()
  const { submit, submitting } = useChangePassword()
  const { errorMessage, clearError, showApiError } = useFormApiErrors(form, changePasswordFields)
  const { message } = App.useApp()
  const { user } = useAuth()

  const handleFinish = async (values: ChangePasswordRequest) => {
    clearError()
    try {
      if (await submit(values)) {
        form.resetFields()
        message.success('Password changed.')
      }
    } catch (error) {
      if (error instanceof ApiError && error.errorCode === 'INCORRECT_PASSWORD') {
        form.setFields([{ name: 'currentPassword', errors: [error.message] }])
        return
      }
      showApiError(error)
    }
  }

  return (
    <>
      {errorMessage && (
        <Alert type="error" showIcon title={errorMessage} style={{ marginBottom: 24, maxWidth: 720 }} />
      )}

      <Form<ChangePasswordRequest>
        form={form}
        layout="vertical"
        onFinish={handleFinish}
        disabled={submitting}
        style={{ maxWidth: 720 }}
      >
        {/* Tells password managers which saved login the new password belongs to. */}
        <input type="text" name="username" autoComplete="username" value={user?.email ?? ''} readOnly hidden />

        <Form.Item
          label="Current password"
          name="currentPassword"
          rules={[{ required: true, whitespace: true, message: 'Current password is required' }]}
        >
          <Input.Password autoComplete="current-password" />
        </Form.Item>

        <Form.Item
          label="New password"
          name="newPassword"
          extra={`At least ${PASSWORD_MIN_LENGTH} characters.`}
          rules={newPasswordFieldRules}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>

        <Form.Item
          label="Confirm new password"
          name="confirmPassword"
          dependencies={['newPassword']}
          rules={confirmNewPasswordRules}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>

        <Form.Item>
          <Button type="primary" htmlType="submit" loading={submitting}>
            Change Password
          </Button>
        </Form.Item>
      </Form>
    </>
  )
}

export default ChangePasswordForm
