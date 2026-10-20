import { Alert, App, Button, Flex, Form, Input } from 'antd'
import type { ClientProfile, UpdateProfileRequest } from '../api/profile'
import { useFormApiErrors } from '../hooks/useFormApiErrors'
import { useUpdateProfile } from '../hooks/useUpdateProfile'

interface ProfileFormValues {
  firstName: string
  lastName: string
  phone?: string
}

interface ProfileFormProps {
  profile: ClientProfile
}

const NAME_MAX_LENGTH = 100
const PHONE_MAX_LENGTH = 30
// Matches the API: digits, spaces, parentheses and hyphens, with an optional leading +, starting and ending on a digit.
const PHONE_PATTERN = /^\+?[0-9][0-9 ()-]*[0-9]$/

const profileFormFields: readonly (keyof ProfileFormValues)[] = ['firstName', 'lastName', 'phone']

// Only the fields that differ from the loaded profile, trimmed. A cleared phone is sent as null.
function changedFields(profile: ClientProfile, { firstName, lastName, phone }: ProfileFormValues) {
  const next = { firstName: firstName.trim(), lastName: lastName.trim(), phone: phone?.trim() || null }
  const request: UpdateProfileRequest = {}
  if (next.firstName !== profile.firstName) request.firstName = next.firstName
  if (next.lastName !== profile.lastName) request.lastName = next.lastName
  if (next.phone !== profile.phone) request.phone = next.phone
  return request
}

const nameRules = (label: string) => [
  { required: true, whitespace: true, message: `${label} is required` },
  { max: NAME_MAX_LENGTH, message: `${label} must not exceed ${NAME_MAX_LENGTH} characters` },
]

// Edits the client's name and phone; email is shown read-only. Save sends only the changed fields, Cancel restores
// the loaded values. Remount with a new `profile` to start from it.
function ProfileForm({ profile }: ProfileFormProps) {
  const [form] = Form.useForm<ProfileFormValues>()
  const { submit, submitting } = useUpdateProfile()
  const { errorMessage, clearError, showApiError } = useFormApiErrors(form, profileFormFields)
  const { message } = App.useApp()

  const handleFinish = async (values: ProfileFormValues) => {
    const request = changedFields(profile, values)
    if (Object.keys(request).length === 0) {
      message.info('No changes to save.')
      return
    }
    clearError()
    try {
      if (await submit(request)) message.success('Profile updated.')
    } catch (error) {
      showApiError(error)
    }
  }

  const handleCancel = () => {
    form.resetFields()
    clearError()
  }

  return (
    <>
      {errorMessage && (
        <Alert type="error" showIcon title={errorMessage} style={{ marginBottom: 24, maxWidth: 720 }} />
      )}

      <Form<ProfileFormValues>
        form={form}
        layout="vertical"
        initialValues={{ firstName: profile.firstName, lastName: profile.lastName, phone: profile.phone ?? undefined }}
        onFinish={handleFinish}
        disabled={submitting}
        style={{ maxWidth: 720 }}
      >
        <Form.Item label="First name" name="firstName" rules={nameRules('First name')}>
          <Input autoComplete="given-name" />
        </Form.Item>

        <Form.Item label="Last name" name="lastName" rules={nameRules('Last name')}>
          <Input autoComplete="family-name" />
        </Form.Item>

        <Form.Item label="Email" htmlFor="profile-email" extra="Your email address can't be changed.">
          <Input id="profile-email" value={profile.email} readOnly variant="filled" />
        </Form.Item>

        <Form.Item
          label="Phone number"
          name="phone"
          rules={[
            { max: PHONE_MAX_LENGTH, message: `Phone number must not exceed ${PHONE_MAX_LENGTH} characters` },
            {
              validator: (_, value?: string) =>
                !value?.trim() || PHONE_PATTERN.test(value.trim())
                  ? Promise.resolve()
                  : Promise.reject(
                      new Error('Phone number may only contain digits, spaces, parentheses, hyphens and a leading +'),
                    ),
            },
          ]}
        >
          <Input type="tel" autoComplete="tel" />
        </Form.Item>

        <Form.Item>
          <Flex gap="small">
            <Button type="primary" htmlType="submit" loading={submitting}>
              Save
            </Button>
            <Button onClick={handleCancel}>Cancel</Button>
          </Flex>
        </Form.Item>
      </Form>
    </>
  )
}

export default ProfileForm
