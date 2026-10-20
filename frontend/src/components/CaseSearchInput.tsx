import { Input } from 'antd'
import { SearchOutlined } from '@ant-design/icons'

interface CaseSearchInputProps {
  value: string
  compact: boolean
  onChange: (value: string) => void
}

// Text box for searching the case list by title; the clear button empties it.
function CaseSearchInput({ value, compact, onChange }: CaseSearchInputProps) {
  return (
    <Input
      aria-label="Search cases by title"
      placeholder="Search by title"
      prefix={<SearchOutlined />}
      allowClear
      value={value}
      onChange={(event) => onChange(event.target.value)}
      style={compact ? { width: '100%' } : { width: 280 }}
    />
  )
}

export default CaseSearchInput
