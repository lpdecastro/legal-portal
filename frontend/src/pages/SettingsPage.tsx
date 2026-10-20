import { Typography } from 'antd'
import ChangePasswordForm from '../components/ChangePasswordForm'

function SettingsPage() {
  return (
    <>
      <Typography.Title level={2}>Settings</Typography.Title>
      <Typography.Title level={4}>Change password</Typography.Title>
      <Typography.Paragraph type="secondary">
        Enter your current password, then choose a new one.
      </Typography.Paragraph>
      <ChangePasswordForm />
    </>
  )
}

export default SettingsPage
