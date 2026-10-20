import { Button, Result } from 'antd'
import { useNavigate } from 'react-router'
import { paths } from '../routes/paths'

// Shown for missing or inaccessible cases; doesn't reveal which.
function CaseNotFound() {
  const navigate = useNavigate()

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

export default CaseNotFound
