import { Alert, Button, Empty, Flex, Select } from 'antd'
import { useLegalCategories } from '../hooks/useLegalCategories'

interface LegalCategorySelectProps {
  // `id`, `value` and `onChange` are injected by an antd `Form.Item`, so the select works as a form control.
  id?: string
  value?: string
  onChange?: (value: string) => void
  disabled?: boolean
}

// Dropdown of the active legal categories, for the Create/Edit Draft forms. The value is the category ID.
function LegalCategorySelect({ id, value, onChange, disabled }: LegalCategorySelectProps) {
  const { categories, loading, error, retry } = useLegalCategories()
  const options = categories.map((category) => ({ value: category.id, label: category.name }))
  // Hide the raw ID of a preselected category until its name is known.
  const ready = !loading && !error

  return (
    <Flex vertical gap="small">
      <Select
        id={id}
        value={ready ? value : undefined}
        onChange={onChange}
        options={options}
        loading={loading}
        disabled={disabled || !ready}
        status={error ? 'error' : undefined}
        placeholder={loading ? 'Loading categories…' : 'Select a legal category'}
        notFoundContent={<Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No legal categories available" />}
      />
      {error && (
        <Alert
          type="error"
          showIcon
          title="We couldn't load the legal categories."
          action={
            <Button size="small" onClick={retry}>
              Retry
            </Button>
          }
        />
      )}
    </Flex>
  )
}

export default LegalCategorySelect
