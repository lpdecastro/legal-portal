import { useState } from 'react'
import { Alert, App, Button, Flex, Form, Input, Typography } from 'antd'
import { Link, useNavigate } from 'react-router'
import type { RegisterRequest } from '../api/auth'
import { ApiError } from '../api/client'
import { useRegister } from '../hooks/useRegister'
import { paths } from '../routes/paths'
import { confirmPasswordRules, newPasswordRules, PASSWORD_MIN_LENGTH } from '../utils/passwordRules'

interface RegisterFormValues extends RegisterRequest {
  confirmPassword: string
}

type RegisterField = keyof RegisterFormValues

const passwordRules = newPasswordRules('Password is required')
const passwordConfirmationRules = confirmPasswordRules('password', 'Please confirm your password')

const formFields: readonly RegisterField[] = ['firstName', 'lastName', 'email', 'password']

function isFormField(field: string): field is RegisterField {
  return (formFields as readonly string[]).includes(field)
}

function RegisterPage() {
  const [form] = Form.useForm<RegisterFormValues>()
  const { submit, submitting } = useRegister()
  const [errorMessage, setErrorMessage] = useState<string>()
  const navigate = useNavigate()
  const { message } = App.useApp()

  // Maps an API error onto form fields where possible, otherwise shows it in the alert.
  const showApiError = (error: unknown) => {
    if (!(error instanceof ApiError)) {
      setErrorMessage('Something went wrong. Please try again.')
      return
    }
    if (error.errorCode === 'EMAIL_ALREADY_EXISTS') {
      form.setFields([{ name: 'email', errors: [error.message] }])
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

  const handleFinish = async ({ firstName, lastName, email, password }: RegisterFormValues) => {
    setErrorMessage(undefined)
    try {
      await submit({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        password,
      })
    } catch (error) {
      showApiError(error)
      return
    }
    message.success('Account created. Please log in.')
    navigate(paths.login)
  }

  return (
    <>
      <Typography.Title level={2}>Create an account</Typography.Title>
      <Typography.Paragraph type="secondary">
        Register to submit and track your legal cases.
      </Typography.Paragraph>

      {errorMessage && (
        <Alert type="error" showIcon title={errorMessage} style={{ marginBottom: 24 }} />
      )}

      <Form<RegisterFormValues>
        form={form}
        layout="vertical"
        requiredMark={false}
        onFinish={handleFinish}
        disabled={submitting}
      >
        <Flex gap="middle">
          <Form.Item
            label="First name"
            name="firstName"
            style={{ flex: 1 }}
            rules={[
              { required: true, whitespace: true, message: 'First name is required' },
              { max: 100, message: 'First name must not exceed 100 characters' },
            ]}
          >
            <Input autoComplete="given-name" />
          </Form.Item>
          <Form.Item
            label="Last name"
            name="lastName"
            style={{ flex: 1 }}
            rules={[
              { required: true, whitespace: true, message: 'Last name is required' },
              { max: 100, message: 'Last name must not exceed 100 characters' },
            ]}
          >
            <Input autoComplete="family-name" />
          </Form.Item>
        </Flex>

        <Form.Item
          label="Email"
          name="email"
          normalize={(value: string) => value.trim()}
          rules={[
            { required: true, message: 'Email is required' },
            { type: 'email', message: 'Invalid email address' },
            { max: 254, message: 'Email must not exceed 254 characters' },
          ]}
        >
          <Input type="email" autoComplete="email" />
        </Form.Item>

        <Form.Item
          label="Password"
          name="password"
          extra={`At least ${PASSWORD_MIN_LENGTH} characters.`}
          rules={passwordRules}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>

        <Form.Item
          label="Confirm password"
          name="confirmPassword"
          dependencies={['password']}
          rules={passwordConfirmationRules}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>

        <Form.Item style={{ marginBottom: 16 }}>
          <Button type="primary" htmlType="submit" block loading={submitting}>
            Create account
          </Button>
        </Form.Item>
      </Form>

      <Typography.Text type="secondary">
        Already have an account? <Link to={paths.login}>Log in</Link>
      </Typography.Text>
    </>
  )
}

export default RegisterPage
