import { App, Avatar, Button, Dropdown, Flex, Typography } from 'antd'
import type { MenuProps } from 'antd'
import { LogoutOutlined, SettingOutlined, UserOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router'
import { useAuth } from '../hooks/useAuth'
import { useLogout } from '../hooks/useLogout'
import { paths } from '../routes/paths'

const LOGOUT_ERROR_MESSAGE = "We couldn't log you out. Please try again."

interface ProfileMenuProps {
  showName?: boolean
}

function ProfileMenu({ showName = true }: ProfileMenuProps) {
  const navigate = useNavigate()
  const { message } = App.useApp()
  const { user } = useAuth()
  const { submit: logout, loggingOut } = useLogout()
  const name = user ? `${user.firstName} ${user.lastName}` : 'My Account'

  const handleLogout = () => {
    logout().catch(() => {
      message.error(LOGOUT_ERROR_MESSAGE)
    })
  }

  const items: MenuProps['items'] = [
    { key: 'profile', icon: <UserOutlined />, label: 'Profile', onClick: () => navigate(paths.profile) },
    { key: 'settings', icon: <SettingOutlined />, label: 'Settings', onClick: () => navigate(paths.settings) },
    { type: 'divider' },
    { key: 'logout', icon: <LogoutOutlined />, label: 'Log out', disabled: loggingOut, onClick: handleLogout },
  ]

  return (
    <Dropdown menu={{ items }} trigger={['click']} placement="bottomRight" disabled={loggingOut}>
      <Button
        type="text"
        aria-label={loggingOut ? 'Logging out' : 'Open profile menu'}
        loading={loggingOut}
        style={{ height: 'auto', paddingBlock: 4 }}
      >
        <Flex align="center" gap="small">
          <Avatar size="small" icon={<UserOutlined />} />
          {showName && <Typography.Text>{name}</Typography.Text>}
        </Flex>
      </Button>
    </Dropdown>
  )
}

export default ProfileMenu
