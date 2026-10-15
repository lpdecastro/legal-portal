import { useState } from 'react'
import { Button, Drawer, Flex, Grid, Layout, theme } from 'antd'
import { MenuOutlined } from '@ant-design/icons'
import { Outlet } from 'react-router'
import BrandLogo from '../components/BrandLogo'
import ProfileMenu from '../components/ProfileMenu'
import SidebarNav from '../components/SidebarNav'

const SIDER_WIDTH = 240

function AuthenticatedLayout() {
  const { token } = theme.useToken()
  const screens = Grid.useBreakpoint()
  const isDesktop = screens.lg ?? true
  const [drawerOpen, setDrawerOpen] = useState(false)

  return (
    <Layout style={{ minHeight: '100svh' }}>
      {isDesktop && (
        <Layout.Sider
          width={SIDER_WIDTH}
          style={{
            position: 'sticky',
            top: 0,
            height: '100svh',
            borderInlineEnd: `1px solid ${token.colorBorderSecondary}`,
          }}
        >
          <Flex align="center" style={{ height: 64, paddingInline: token.paddingLG }}>
            <BrandLogo />
          </Flex>
          <SidebarNav />
        </Layout.Sider>
      )}

      {!isDesktop && (
        <Drawer
          placement="left"
          open={drawerOpen}
          onClose={() => setDrawerOpen(false)}
          title={<BrandLogo />}
          size={SIDER_WIDTH}
          styles={{ body: { padding: 0 } }}
        >
          <SidebarNav onNavigate={() => setDrawerOpen(false)} />
        </Drawer>
      )}

      <Layout>
        <Layout.Header
          style={{
            position: 'sticky',
            top: 0,
            zIndex: 10,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: token.margin,
            paddingInline: isDesktop ? token.paddingLG : token.padding,
            borderBlockEnd: `1px solid ${token.colorBorderSecondary}`,
          }}
        >
          {isDesktop ? (
            <span />
          ) : (
            <Flex align="center" gap="small">
              <Button
                type="text"
                icon={<MenuOutlined />}
                aria-label="Open navigation"
                onClick={() => setDrawerOpen(true)}
              />
              <BrandLogo />
            </Flex>
          )}
          <ProfileMenu showName={isDesktop} />
        </Layout.Header>

        <Layout.Content style={{ padding: isDesktop ? token.paddingLG : token.padding }}>
          <Outlet />
        </Layout.Content>
      </Layout>
    </Layout>
  )
}

export default AuthenticatedLayout
