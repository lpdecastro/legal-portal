import { Alert, Button, Card, Empty, Flex, Grid, Spin, Table, Typography } from 'antd'
import type { TableColumnsType } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import type { CaseListRequest, CaseStatus, CaseSummary } from '../api/cases'
import CasePagination from '../components/CasePagination'
import CaseStatusFilter from '../components/CaseStatusFilter'
import CaseStatusTag from '../components/CaseStatusTag'
import { useCases } from '../hooks/useCases'
import { caseDetailsPath, paths } from '../routes/paths'
import { formatDate } from '../utils/formatDate'

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
  loading: boolean
}

// Desktop: a table whose rows open the case.
function CaseTable({ cases, loading }: CaseListProps) {
  const navigate = useNavigate()

  return (
    <Table<CaseSummary>
      rowKey="id"
      columns={columns}
      dataSource={cases}
      loading={loading}
      pagination={false}
      onRow={({ id }) => ({ onClick: () => navigate(caseDetailsPath(id)), style: { cursor: 'pointer' } })}
    />
  )
}

// Mobile: one card per case, each a link to the case.
function CaseCards({ cases, loading }: CaseListProps) {
  return (
    <Spin spinning={loading}>
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
    </Spin>
  )
}

const DEFAULT_PAGE_SIZE = 10

const STATUS_NAMES: Record<CaseStatus, string> = { DRAFT: 'draft', SUBMITTED: 'submitted', CLOSED: 'closed' }

function MyCasesContent() {
  const [request, setRequest] = useState<CaseListRequest>({ page: 0, size: DEFAULT_PAGE_SIZE })
  const { data, loading, error, retry } = useCases(request)
  const screens = Grid.useBreakpoint()
  const isDesktop = screens.md ?? true
  const navigate = useNavigate()

  // A page past the end (e.g. its last case was deleted elsewhere) moves back to the last page.
  // Adjusted while rendering, as React recommends for state derived from a response.
  if (!loading && data && data.content.length === 0 && data.page.number > 0 && data.page.totalPages > 0) {
    setRequest({ ...request, page: data.page.totalPages - 1, size: data.page.size })
  }

  // A new filter starts again from the first page.
  const changeStatus = (status?: CaseStatus) => setRequest({ page: 0, size: request.size, status })

  if (!data && loading) {
    return (
      <Flex justify="center" style={{ padding: 48 }}>
        <Spin size="large" description="Loading cases…" />
      </Flex>
    )
  }

  // With no cases at all there is nothing to filter, so the filter is left out.
  if (!request.status && !loading && !error && data?.page.totalElements === 0) {
    return (
      <Empty description="You don't have any cases yet." style={{ padding: 48 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(paths.createCase)}>
          Create Case
        </Button>
      </Empty>
    )
  }

  return (
    <>
      <div style={{ marginBottom: 16 }}>
        <CaseStatusFilter value={request.status} compact={!isDesktop} onChange={changeStatus} />
      </div>
      {renderCases()}
    </>
  )

  function renderCases() {
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

    if (!data) return null

    if (data.page.totalElements === 0) {
      return (
        <Flex justify="center" style={{ padding: 48 }}>
          {/* The previous filter matched nothing; wait for the new one rather than repeat its empty state. */}
          {loading || !request.status ? (
            <Spin size="large" description="Loading cases…" />
          ) : (
            <Empty description={`No ${STATUS_NAMES[request.status]} cases found.`}>
              <Button onClick={() => changeStatus(undefined)}>Show All Statuses</Button>
            </Empty>
          )}
        </Flex>
      )
    }

    return (
      <>
        {isDesktop ? (
          <CaseTable cases={data.content} loading={loading} />
        ) : (
          <CaseCards cases={data.content} loading={loading} />
        )}
        <CasePagination
          page={data.page}
          disabled={loading}
          compact={!isDesktop}
          onChange={(page, size) => setRequest({ ...request, page, size })}
        />
      </>
    )
  }
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
