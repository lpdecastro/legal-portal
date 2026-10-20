import { Typography } from 'antd'
import CaseStatistics from '../components/CaseStatistics'

function DashboardPage() {
  return (
    <>
      <Typography.Title level={2}>Dashboard</Typography.Title>
      <Typography.Paragraph type="secondary">An overview of your cases.</Typography.Paragraph>
      <CaseStatistics />
    </>
  )
}

export default DashboardPage
