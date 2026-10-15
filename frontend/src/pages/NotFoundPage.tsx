import { Button, Flex, Result } from 'antd'
import { useNavigate } from 'react-router'
import { paths } from '../routes/paths'

function NotFoundPage() {
  const navigate = useNavigate()

  return (
    <Flex align="center" justify="center" style={{ minHeight: '100svh' }}>
      <Result
        status="404"
        title="Page Not Found"
        subTitle="The page you are looking for does not exist."
        extra={
          <Button type="primary" onClick={() => navigate(paths.dashboard)}>
            Back to Dashboard
          </Button>
        }
      />
    </Flex>
  )
}

export default NotFoundPage
