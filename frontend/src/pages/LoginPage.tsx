import { useState } from 'react'
import { Alert, Button, Form, Input, Typography } from 'antd'
import { Link, useNavigate } from 'react-router'
import type { LoginRequest } from '../api/auth'
import { ApiError } from '../api/client'
import { useLogin } from '../hooks/useLogin'
import { paths } from '../routes/paths'

type LoginField = keyof LoginRequest

// Shown for every 401 so the form never reveals whether an email is registered.
const INVALID_CREDENTIALS_MESSAGE = 'Invalid email or password.'
const GENERIC_ERROR_MESSAGE = 'Something went wrong. Please try again.'

const formFields: readonly LoginField[] = ['email', 'password']

function isFormField(field: string): field is LoginField {
  return (formFields as readonly string[]).includes(field)
}

function LoginPage() {
  const [form] = Form.useForm<LoginRequest>()
  const { submit, submitting } = useLogin()
  const [errorMessage, setErrorMessage] = useState<string>()
  const navigate = useNavigate()

  // Maps an API error onto form fields where possible, otherwise shows it in the alert.
  const showApiError = (error: unknown) => {
    if (!(error instanceof ApiError)) {
      setErrorMessage(GENERIC_ERROR_MESSAGE)
      return
    }
    if (error.status === 401) {
      setErrorMessage(INVALID_CREDENTIALS_MESSAGE)
      return
    }
    const fieldErrors = error.problem?.fieldErrors ?? []
    const known = fieldErrors.flatMap(({ field, message }) =>
      isFormField(field) ? [{ name: field, errors: [message] }] : [],
    )
    form.setFields(known)
    if (known.length === 0 || known.length < fieldErrors.length) {
      setErrorMessage(error.message)
    }
  }

  const handleFinish = async ({ email, password }: LoginRequest) => {
    setErrorMessage(undefined)
    try {
      await submit({ email: email.trim(), password })
    } catch (error) {
      showApiError(error)
      return
    }
    navigate(paths.dashboard, { replace: true })
  }

  return (
    <>
      <Typography.Title level={2}>Log in</Typography.Title>
      <Typography.Paragraph type="secondary">
        Sign in to manage your legal cases.
      </Typography.Paragraph>

      {errorMessage && (
        <Alert type="error" showIcon title={errorMessage} style={{ marginBottom: 24 }} />
      )}

      <Form<LoginRequest>
        form={form}
        layout="vertical"
        requiredMark={false}
        onFinish={handleFinish}
        disabled={submitting}
      >
        <Form.Item
          label="Email"
          name="email"
          normalize={(value: string) => value.trim()}
          rules={[
            { required: true, message: 'Email is required' },
            { type: 'email', message: 'Invalid email address' },
          ]}
        >
          <Input type="email" autoComplete="email" />
        </Form.Item>

        <Form.Item
          label="Password"
          name="password"
          rules={[{ required: true, message: 'Password is required' }]}
        >
          <Input.Password autoComplete="current-password" />
        </Form.Item>

        <Form.Item style={{ marginBottom: 16 }}>
          <Button type="primary" htmlType="submit" block loading={submitting}>
            Log in
          </Button>
        </Form.Item>
      </Form>

      <Typography.Text type="secondary">
        Don&apos;t have an account? <Link to={paths.register}>Create one</Link>
      </Typography.Text>
    </>
  )
}

export default LoginPage
