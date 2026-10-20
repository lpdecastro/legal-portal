import { Alert, Button, Card, Empty, Flex, Grid, Spin, Table, Typography } from 'antd'
import type { TableColumnsType } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { Link, useNavigate } from 'react-router'
import type { RecentCase } from '../api/dashboard'
import { useDashboardSummary } from '../hooks/useDashboardSummary'
import { caseDetailsPath, paths } from '../routes/paths'
import { formatDate } from '../utils/formatDate'
import CaseStatusTag from './CaseStatusTag'

const columns: TableColumnsType<RecentCase> = [
  {
    title: 'Title',
    dataIndex: 'title',
    // The link keeps rows keyboard-accessible; it stops the click so the row doesn't navigate a second time.
    render: (title: string, { id }) => (
      <Link to={caseDetailsPath(id)} onClick={(event) => event.stopPropagation()}>
        {title}
      </Link>
    ),
  },
  { title: 'Status', dataIndex: 'status', render: (_, { status }) => <CaseStatusTag status={status} /> },
  { title: 'Created', dataIndex: 'createdAt', render: (createdAt: string) => formatDate(createdAt) },
]

interface RecentCaseListProps {
  cases: RecentCase[]
}

// Desktop: a table whose rows open the case.
function RecentCaseTable({ cases }: RecentCaseListProps) {
  const navigate = useNavigate()

  return (
    <Table<RecentCase>
      rowKey="id"
      columns={columns}
      dataSource={cases}
      pagination={false}
      onRow={({ id }) => ({ onClick: () => navigate(caseDetailsPath(id)), style: { cursor: 'pointer' } })}
    />
  )
}

// Mobile: one card per case, each a link to the case.
function RecentCaseCards({ cases }: RecentCaseListProps) {
  return (
    <Flex vertical gap="small">
      {cases.map((legalCase) => (
        <Link key={legalCase.id} to={caseDetailsPath(legalCase.id)}>
          <Card size="small" hoverable>
            <Flex justify="space-between" align="start" gap="small">
              <Typography.Text strong>{legalCase.title}</Typography.Text>
              <CaseStatusTag status={legalCase.status} />
            </Flex>
            <Typography.Text type="secondary">{formatDate(legalCase.createdAt)}</Typography.Text>
          </Card>
        </Link>
      ))}
    </Flex>
  )
}

function RecentCasesContent() {
  const { data, isPending, isFetching, refetch } = useDashboardSummary()
  const screens = Grid.useBreakpoint()
  const isDesktop = screens.md ?? true
  const navigate = useNavigate()

  if (isPending) {
    return (
      <Flex justify="center" style={{ padding: 48 }}>
        <Spin size="large" description="Loading recent cases…" />
      </Flex>
    )
  }

  // A failed refresh keeps showing the last cases; only a summary that never loaded shows the error.
  if (!data) {
    return (
      <Alert
        type="error"
        showIcon
        title="We couldn't load your recent cases."
        description="Please check your connection and try again."
        action={
          <Button size="small" loading={isFetching} onClick={() => refetch()}>
            Retry
          </Button>
        }
      />
    )
  }

  if (data.recentCases.length === 0) {
    return (
      <Empty description="You don't have any cases yet." style={{ padding: 24 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(paths.createCase)}>
          Create Case
        </Button>
      </Empty>
    )
  }

  // The API returns the 5 newest cases, newest first.
  return isDesktop ? <RecentCaseTable cases={data.recentCases} /> : <RecentCaseCards cases={data.recentCases} />
}

// The client's newest cases on the dashboard, read from the shared dashboard summary.
function RecentCases() {
  return (
    <Card title="Recent Cases" extra={<Link to={paths.cases}>View all</Link>} style={{ marginTop: 24 }}>
      <RecentCasesContent />
    </Card>
  )
}

export default RecentCases
