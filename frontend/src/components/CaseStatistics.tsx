import { Alert, Button, Card, Col, Flex, Row, Spin, Statistic } from 'antd'
import type { CaseStatistics as CaseStatisticsData } from '../api/dashboard'
import { useDashboardSummary } from '../hooks/useDashboardSummary'

// Labels match CaseStatusTag.
const STATISTICS: { key: keyof CaseStatisticsData; label: string }[] = [
  { key: 'total', label: 'Total Cases' },
  { key: 'draft', label: 'Draft' },
  { key: 'submitted', label: 'Submitted' },
  { key: 'closed', label: 'Closed' },
]

// The client's case counts: two per row on mobile, four across from `lg`.
function CaseStatistics() {
  const { data, isPending, isFetching, refetch } = useDashboardSummary()

  if (isPending) {
    return (
      <Flex justify="center" style={{ padding: 48 }}>
        <Spin size="large" description="Loading statistics…" />
      </Flex>
    )
  }

  // A failed refresh keeps showing the last counts; only a summary that never loaded shows the error.
  if (!data) {
    return (
      <Alert
        type="error"
        showIcon
        title="We couldn't load your case statistics."
        description="Please check your connection and try again."
        action={
          <Button size="small" loading={isFetching} onClick={() => refetch()}>
            Retry
          </Button>
        }
        style={{ maxWidth: 720 }}
      />
    )
  }

  return (
    <Row gutter={[16, 16]}>
      {STATISTICS.map(({ key, label }) => (
        <Col key={key} xs={12} lg={6}>
          <Card>
            <Statistic title={label} value={data.statistics[key]} />
          </Card>
        </Col>
      ))}
    </Row>
  )
}

export default CaseStatistics
