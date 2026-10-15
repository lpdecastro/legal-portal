import { Card, Flex, Layout, Typography, theme } from 'antd'
import { Outlet } from 'react-router'
import BrandLogo from '../components/BrandLogo'

function PublicLayout() {
  const { token } = theme.useToken()

  return (
    <Layout style={{ minHeight: '100svh' }}>
      <Layout.Content>
        <Flex
          vertical
          align="center"
          justify="center"
          gap="large"
          style={{ minHeight: '100svh', padding: token.paddingLG }}
        >
          <BrandLogo size="large" />
          <Card style={{ width: '100%', maxWidth: 440 }}>
            <Outlet />
          </Card>
          <Typography.Text type="secondary">
            © {new Date().getFullYear()} Legal Portal
          </Typography.Text>
        </Flex>
      </Layout.Content>
    </Layout>
  )
}

export default PublicLayout
