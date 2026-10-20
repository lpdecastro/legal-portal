import { Alert, Button, Card, Descriptions, Flex, Spin, Typography } from 'antd'
import type { DescriptionsProps } from 'antd'
import { ArrowLeftOutlined, CloseCircleOutlined, DeleteOutlined, EditOutlined, SendOutlined } from '@ant-design/icons'
import { Link, useNavigate, useParams } from 'react-router'
import { isCaseNotFound } from '../api/cases'
import type { LegalCase } from '../api/cases'
import CaseNotFound from '../components/CaseNotFound'
import CaseStatusTag from '../components/CaseStatusTag'
import { useCase } from '../hooks/useCase'
import { useLegalCategories } from '../hooks/useLegalCategories'
import { editCasePath, paths } from '../routes/paths'
import { formatDate } from '../utils/formatDate'

const NOT_PROVIDED = <Typography.Text type="secondary">Not provided</Typography.Text>

// The case only carries a category ID; the name comes from the active categories.
function CategoryName({ categoryId }: { categoryId: string }) {
  const { categories, loading } = useLegalCategories()

  if (loading) return <Spin size="small" />
  const category = categories.find(({ id }) => id === categoryId)
  return category ? category.name : <Typography.Text type="secondary">Unavailable</Typography.Text>
}

// Edit, Delete and Submit for drafts; Close for submitted cases; nothing once closed.
// Delete, Submit and Close stay disabled until their flows land (CASE-06-02, CASE-07-02, CASE-08-02).
function CaseActions({ legalCase }: { legalCase: LegalCase }) {
  const navigate = useNavigate()

  if (legalCase.status === 'DRAFT') {
    return (
      <Flex wrap gap="small">
        <Button type="primary" icon={<EditOutlined />} onClick={() => navigate(editCasePath(legalCase.id))}>
          Edit
        </Button>
        <Button icon={<SendOutlined />} disabled>
          Submit
        </Button>
        <Button danger icon={<DeleteOutlined />} disabled>
          Delete
        </Button>
      </Flex>
    )
  }

  if (legalCase.status === 'SUBMITTED') {
    return (
      <Button icon={<CloseCircleOutlined />} disabled>
        Close
      </Button>
    )
  }

  return null
}

function CaseDetails({ legalCase }: { legalCase: LegalCase }) {
  const items: DescriptionsProps['items'] = [
    { key: 'category', label: 'Category', children: <CategoryName categoryId={legalCase.categoryId} /> },
    { key: 'location', label: 'Location', children: legalCase.location ?? NOT_PROVIDED },
    { key: 'createdAt', label: 'Created', children: formatDate(legalCase.createdAt) },
    { key: 'updatedAt', label: 'Last updated', children: formatDate(legalCase.updatedAt) },
    {
      key: 'description',
      label: 'Description',
      span: 'filled',
      children: legalCase.description ? (
        <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>
          {legalCase.description}
        </Typography.Paragraph>
      ) : (
        NOT_PROVIDED
      ),
    },
  ]

  return (
    <Flex vertical gap="middle" style={{ maxWidth: 960 }}>
      <Flex justify="space-between" align="start" wrap gap="middle">
        <Flex vertical gap="small">
          <Typography.Title level={2} style={{ margin: 0, overflowWrap: 'anywhere' }}>
            {legalCase.title}
          </Typography.Title>
          <div>
            <CaseStatusTag status={legalCase.status} />
          </div>
        </Flex>
        <CaseActions legalCase={legalCase} />
      </Flex>
      <Card>
        <Descriptions column={{ xs: 1, sm: 1, md: 2 }} items={items} />
      </Card>
    </Flex>
  )
}

function CaseDetailsContent({ id }: { id: string }) {
  const { legalCase, loading, error, retry } = useCase(id)

  if (loading) {
    return (
      <Flex justify="center" style={{ padding: 48 }}>
        <Spin size="large" description="Loading case…" />
      </Flex>
    )
  }

  if (isCaseNotFound(error)) {
    return <CaseNotFound />
  }

  if (!legalCase) {
    return (
      <Alert
        type="error"
        showIcon
        title="We couldn't load this case."
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

  return <CaseDetails legalCase={legalCase} />
}

function CaseDetailsPage() {
  const { id = '' } = useParams()

  return (
    <>
      <div style={{ marginBottom: 16 }}>
        <Link to={paths.cases}>
          <ArrowLeftOutlined /> Back to My Cases
        </Link>
      </div>
      {/* Keyed so a different case starts with a fresh load. */}
      <CaseDetailsContent key={id} id={id} />
    </>
  )
}

export default CaseDetailsPage
