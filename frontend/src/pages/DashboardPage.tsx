import { Typography } from 'antd'
import CaseStatistics from '../components/CaseStatistics'
import RecentCases from '../components/RecentCases'

function DashboardPage() {
  return (
    <>
      <Typography.Title level={2}>Dashboard</Typography.Title>
      <Typography.Paragraph type="secondary">An overview of your cases.</Typography.Paragraph>
      <CaseStatistics />
      <RecentCases />
    </>
  )
}

export default DashboardPage
