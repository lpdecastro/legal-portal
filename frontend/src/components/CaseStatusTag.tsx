import { Tag } from 'antd'
import type { CaseStatus } from '../api/cases'

const STATUS_TAGS: Record<CaseStatus, { label: string; color: string }> = {
  DRAFT: { label: 'Draft', color: 'default' },
  SUBMITTED: { label: 'Submitted', color: 'processing' },
  CLOSED: { label: 'Closed', color: 'success' },
}

interface CaseStatusTagProps {
  status: CaseStatus
}

function CaseStatusTag({ status }: CaseStatusTagProps) {
  const { label, color } = STATUS_TAGS[status]
  return <Tag color={color}>{label}</Tag>
}

export default CaseStatusTag
