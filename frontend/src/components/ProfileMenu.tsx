import { Avatar, Button, Dropdown, Flex, Typography } from 'antd'
import type { MenuProps } from 'antd'
import { LogoutOutlined, SettingOutlined, UserOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router'
import { paths } from '../routes/paths'

interface ProfileMenuProps {
  showName?: boolean
}

// TODO: show the signed-in client's name and wire logout once auth lands.
function ProfileMenu({ showName = true }: ProfileMenuProps) {
  const navigate = useNavigate()

  const items: MenuProps['items'] = [
    { key: 'profile', icon: <UserOutlined />, label: 'Profile', onClick: () => navigate(paths.profile) },
    { key: 'settings', icon: <SettingOutlined />, label: 'Settings', onClick: () => navigate(paths.settings) },
    { type: 'divider' },
    { key: 'logout', icon: <LogoutOutlined />, label: 'Log out', disabled: true },
  ]

  return (
    <Dropdown menu={{ items }} trigger={['click']} placement="bottomRight">
      <Button type="text" aria-label="Open profile menu" style={{ height: 'auto', paddingBlock: 4 }}>
        <Flex align="center" gap="small">
          <Avatar size="small" icon={<UserOutlined />} />
          {showName && <Typography.Text>My Account</Typography.Text>}
        </Flex>
      </Button>
    </Dropdown>
  )
}

export default ProfileMenu
