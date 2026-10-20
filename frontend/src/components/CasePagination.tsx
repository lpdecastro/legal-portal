import { Flex, Pagination, Select, Typography } from 'antd'
import type { PageMetadata } from '../api/cases'

const PAGE_SIZE_OPTIONS = [10, 20, 50].map((size) => ({ value: size, label: `${size} / page` }))

interface CasePaginationProps {
  page: PageMetadata
  disabled: boolean
  compact: boolean
  // `page` is zero-based, like the API.
  onChange: (page: number, size: number) => void
}

// Page summary and controls for the case list, driven by the API's page metadata.
function CasePagination({ page, disabled, compact, onChange }: CasePaginationProps) {
  const cases = page.totalElements === 1 ? 'case' : 'cases'

  return (
    <Flex vertical={compact} justify="space-between" align="center" gap="small" wrap style={{ marginTop: 16 }}>
      <Typography.Text type="secondary" aria-live="polite">
        Page {page.number + 1} of {page.totalPages} · {page.totalElements} {cases}
      </Typography.Text>
      <Flex align="center" gap="small" wrap justify="center">
        <Pagination
          current={page.number + 1}
          pageSize={page.size}
          total={page.totalElements}
          disabled={disabled}
          size={compact ? 'small' : undefined}
          showLessItems={compact}
          // antd's own size changer is hidden on phones by its CSS, so the Select below is used everywhere.
          showSizeChanger={false}
          onChange={(current) => onChange(current - 1, page.size)}
        />
        <Select
          aria-label="Cases per page"
          value={page.size}
          options={PAGE_SIZE_OPTIONS}
          disabled={disabled}
          size={compact ? 'small' : undefined}
          // A new page size starts again from the first page.
          onChange={(size) => onChange(0, size)}
        />
      </Flex>
    </Flex>
  )
}

export default CasePagination
