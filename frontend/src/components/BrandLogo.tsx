import { Flex, Typography, theme } from 'antd'
import { BankOutlined } from '@ant-design/icons'

interface BrandLogoProps {
  size?: 'default' | 'large'
}

function BrandLogo({ size = 'default' }: BrandLogoProps) {
  const { token } = theme.useToken()
  const fontSize = size === 'large' ? token.fontSizeHeading3 : token.fontSizeLG

  return (
    <Flex align="center" gap={token.marginXS}>
      <BankOutlined style={{ fontSize, color: token.colorPrimary }} />
      <Typography.Text strong style={{ fontSize, whiteSpace: 'nowrap' }}>
        Legal Portal
      </Typography.Text>
    </Flex>
  )
}

export default BrandLogo
