import { Alert, Button, Card, Descriptions, Flex, Spin, Typography } from 'antd'
import type { DescriptionsProps } from 'antd'
import { ArrowLeftOutlined, EditOutlined } from '@ant-design/icons'
import { Link, useNavigate, useParams } from 'react-router'
import { isCaseNotFound } from '../api/cases'
import type { LegalCase } from '../api/cases'
import type { LegalCategory } from '../api/legalCategories'
import CaseNotFound from '../components/CaseNotFound'
import CaseStatusTag from '../components/CaseStatusTag'
import CloseCaseButton from '../components/CloseCaseButton'
import DeleteCaseButton from '../components/DeleteCaseButton'
import SubmitCaseButton from '../components/SubmitCaseButton'
import { useCase } from '../hooks/useCase'
import { useLegalCategories } from '../hooks/useLegalCategories'
import { editCasePath, paths } from '../routes/paths'
import { formatDate } from '../utils/formatDate'

const NOT_PROVIDED = <Typography.Text type="secondary">Not provided</Typography.Text>

// The case only carries a category ID; the name comes from the active categories.
function CategoryName({ category, loading }: { category?: LegalCategory; loading: boolean }) {
  if (loading) return <Spin size="small" />
  return category ? category.name : <Typography.Text type="secondary">Unavailable</Typography.Text>
}

interface CaseActionsProps {
  legalCase: LegalCase
  // See SubmitCaseButton; undefined while the categories are loading.
  categoryActive?: boolean
  categoriesLoading: boolean
  onChanged: (legalCase: LegalCase) => void
  onStale: () => void
}

// Edit, Delete and Submit for drafts; Close Case for submitted cases; nothing once closed.
function CaseActions({ legalCase, categoryActive, categoriesLoading, onChanged, onStale }: CaseActionsProps) {
  const navigate = useNavigate()

  if (legalCase.status === 'DRAFT') {
    return (
      <Flex wrap gap="small">
        <Button type="primary" icon={<EditOutlined />} onClick={() => navigate(editCasePath(legalCase.id))}>
          Edit
        </Button>
        {/* Waits for the categories so the active-category check can run first. */}
        {categoriesLoading ? (
          <Button disabled>Submit</Button>
        ) : (
          <SubmitCaseButton
            legalCase={legalCase}
            categoryActive={categoryActive}
            onSubmitted={onChanged}
            onStale={onStale}
          />
        )}
        <DeleteCaseButton caseId={legalCase.id} />
      </Flex>
    )
  }

  if (legalCase.status === 'SUBMITTED') {
    return <CloseCaseButton caseId={legalCase.id} onClosed={onChanged} onStale={onStale} />
  }

  return null
}

interface CaseDetailsProps {
  legalCase: LegalCase
  onChanged: (legalCase: LegalCase) => void
  onStale: () => void
}

function CaseDetails({ legalCase, onChanged, onStale }: CaseDetailsProps) {
  const { categories, loading: categoriesLoading, error: categoriesError } = useLegalCategories()
  const category = categories.find(({ id }) => id === legalCase.categoryId)
  // Unknown when the categories couldn't be loaded; the submit API checks it anyway.
  const categoryActive = categoriesLoading || categoriesError ? undefined : category !== undefined

  const items: DescriptionsProps['items'] = [
    {
      key: 'category',
      label: 'Category',
      children: <CategoryName category={category} loading={categoriesLoading} />,
    },
    { key: 'location', label: 'Location', children: legalCase.location ?? NOT_PROVIDED },
    { key: 'createdAt', label: 'Created', children: formatDate(legalCase.createdAt) },
    { key: 'updatedAt', label: 'Last updated', children: formatDate(legalCase.updatedAt) },
    ...(legalCase.closedAt ? [{ key: 'closedAt', label: 'Closed on', children: formatDate(legalCase.closedAt) }] : []),
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
        <CaseActions
          legalCase={legalCase}
          categoryActive={categoryActive}
          categoriesLoading={categoriesLoading}
          onChanged={onChanged}
          onStale={onStale}
        />
      </Flex>
      <Card>
        <Descriptions column={{ xs: 1, sm: 1, md: 2 }} items={items} />
      </Card>
    </Flex>
  )
}

function CaseDetailsContent({ id }: { id: string }) {
  const { legalCase, loading, error, retry, replace } = useCase(id)

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

  return <CaseDetails legalCase={legalCase} onChanged={replace} onStale={retry} />
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
