import { Alert, Button, Card, Empty, Flex, Grid, Spin, Table, Typography } from 'antd'
import type { TableColumnsType } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { Link, useNavigate } from 'react-router'
import type { CaseSummary } from '../api/cases'
import CaseStatusTag from '../components/CaseStatusTag'
import { useCases } from '../hooks/useCases'
import { caseDetailsPath, paths } from '../routes/paths'

const dateFormat = new Intl.DateTimeFormat('en-US', { dateStyle: 'medium' })

const formatDate = (iso: string): string => dateFormat.format(new Date(iso))

const columns: TableColumnsType<CaseSummary> = [
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
  { title: 'Category', dataIndex: 'category' },
  { title: 'Status', dataIndex: 'status', render: (_, { status }) => <CaseStatusTag status={status} /> },
  { title: 'Created', dataIndex: 'createdAt', render: (createdAt: string) => formatDate(createdAt) },
]

interface CaseListProps {
  cases: CaseSummary[]
}

// Desktop: a table whose rows open the case.
function CaseTable({ cases }: CaseListProps) {
  const navigate = useNavigate()

  return (
    <Table<CaseSummary>
      rowKey="id"
      columns={columns}
      dataSource={cases}
      pagination={false}
      onRow={({ id }) => ({ onClick: () => navigate(caseDetailsPath(id)), style: { cursor: 'pointer' } })}
    />
  )
}

// Mobile: one card per case, each a link to the case.
function CaseCards({ cases }: CaseListProps) {
  return (
    <Flex vertical gap="small">
      {cases.map((legalCase) => (
        <Link key={legalCase.id} to={caseDetailsPath(legalCase.id)}>
          <Card size="small" hoverable>
            <Flex justify="space-between" align="start" gap="small">
              <Typography.Text strong>{legalCase.title}</Typography.Text>
              <CaseStatusTag status={legalCase.status} />
            </Flex>
            <Typography.Text type="secondary">
              {legalCase.category} · {formatDate(legalCase.createdAt)}
            </Typography.Text>
          </Card>
        </Link>
      ))}
    </Flex>
  )
}

function MyCasesContent() {
  const { cases, loading, error, retry } = useCases()
  const screens = Grid.useBreakpoint()
  const isDesktop = screens.md ?? true
  const navigate = useNavigate()

  if (loading) {
    return (
      <Flex justify="center" style={{ padding: 48 }}>
        <Spin size="large" description="Loading cases…" />
      </Flex>
    )
  }

  if (error) {
    return (
      <Alert
        type="error"
        showIcon
        title="We couldn't load your cases."
        description="Please check your connection and try again."
        action={
          <Button size="small" onClick={retry}>
            Retry
          </Button>
        }
        style={{ maxWidth: 720 }}
      />
    )
  }

  if (cases.length === 0) {
    return (
      <Empty description="You don't have any cases yet." style={{ padding: 48 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(paths.createCase)}>
          Create Case
        </Button>
      </Empty>
    )
  }

  return isDesktop ? <CaseTable cases={cases} /> : <CaseCards cases={cases} />
}

function MyCasesPage() {
  return (
    <>
      <Typography.Title level={2}>My Cases</Typography.Title>
      <Typography.Paragraph type="secondary">View and track all of your cases.</Typography.Paragraph>
      <MyCasesContent />
    </>
  )
}

export default MyCasesPage
