import { Select } from 'antd'
import type { CaseStatus } from '../api/cases'

// antd's Select can't hold `undefined` as an option value, so "All Statuses" uses a sentinel.
const ALL = 'ALL'

type StatusOption = CaseStatus | typeof ALL

const STATUS_OPTIONS: { value: StatusOption; label: string }[] = [
  { value: ALL, label: 'All Statuses' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'SUBMITTED', label: 'Submitted' },
  { value: 'CLOSED', label: 'Closed' },
]

interface CaseStatusFilterProps {
  // No status means every status.
  value?: CaseStatus
  compact: boolean
  onChange: (status?: CaseStatus) => void
}

// Limits the case list to one status.
function CaseStatusFilter({ value, compact, onChange }: CaseStatusFilterProps) {
  return (
    <Select<StatusOption>
      aria-label="Filter by status"
      value={value ?? ALL}
      options={STATUS_OPTIONS}
      onChange={(selected) => onChange(selected === ALL ? undefined : selected)}
      style={compact ? { width: '100%' } : { width: 200 }}
    />
  )
}

export default CaseStatusFilter
