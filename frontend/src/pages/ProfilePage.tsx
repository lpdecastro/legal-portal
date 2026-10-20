import { Alert, Button, Flex, Spin, Typography } from 'antd'
import ProfileForm from '../components/ProfileForm'
import { useProfile } from '../hooks/useProfile'

function ProfileContent() {
  const { data: profile, isFetching, refetch } = useProfile()

  // Also covers a Retry after a failed load, which keeps the error status until it settles.
  if (!profile && isFetching) {
    return (
      <Flex justify="center" style={{ padding: 48 }}>
        <Spin size="large" description="Loading profile…" />
      </Flex>
    )
  }

  if (!profile) {
    return (
      <Alert
        type="error"
        showIcon
        title="We couldn't load your profile."
        description="Please check your connection and try again."
        action={
          <Button size="small" onClick={() => void refetch()}>
            Retry
          </Button>
        }
        style={{ maxWidth: 720 }}
      />
    )
  }

  // Keyed by the profile's values so a saved or reloaded profile becomes the form's new starting point.
  return <ProfileForm key={JSON.stringify(profile)} profile={profile} />
}

function ProfilePage() {
  return (
    <>
      <Typography.Title level={2}>Profile</Typography.Title>
      <Typography.Paragraph type="secondary">View and update your personal details.</Typography.Paragraph>
      <ProfileContent />
    </>
  )
}

export default ProfilePage
