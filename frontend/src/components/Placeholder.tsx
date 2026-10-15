import type { ReactNode } from 'react'
import { Typography } from 'antd'

interface PlaceholderProps {
  title: string
  children?: ReactNode
}

function Placeholder({ title, children }: PlaceholderProps) {
  return (
    <>
      <Typography.Title level={2}>{title}</Typography.Title>
      <Typography.Paragraph type="secondary">This page is coming soon.</Typography.Paragraph>
      {children}
    </>
  )
}

export default Placeholder
