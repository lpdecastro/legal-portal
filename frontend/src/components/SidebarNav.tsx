import { Menu } from 'antd'
import type { MenuProps } from 'antd'
import {
  DashboardOutlined,
  FileTextOutlined,
  PlusCircleOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Link } from 'react-router'
import { useActiveNavKey } from '../hooks/useActiveNavKey'
import type { NavKey } from '../hooks/useActiveNavKey'
import { paths } from '../routes/paths'

type NavItem = Required<MenuProps>['items'][number] & { key: NavKey }

const navItems: NavItem[] = [
  { key: 'dashboard', icon: <DashboardOutlined />, label: <Link to={paths.dashboard}>Dashboard</Link> },
  { key: 'cases', icon: <FileTextOutlined />, label: <Link to={paths.cases}>My Cases</Link> },
  { key: 'createCase', icon: <PlusCircleOutlined />, label: <Link to={paths.createCase}>Create Case</Link> },
  { key: 'profile', icon: <UserOutlined />, label: <Link to={paths.profile}>Profile</Link> },
  { key: 'settings', icon: <SettingOutlined />, label: <Link to={paths.settings}>Settings</Link> },
]

interface SidebarNavProps {
  onNavigate?: () => void
}

function SidebarNav({ onNavigate }: SidebarNavProps) {
  const activeKey = useActiveNavKey()

  return (
    <Menu
      mode="inline"
      items={navItems}
      selectedKeys={activeKey ? [activeKey] : []}
      onClick={onNavigate}
      style={{ borderInlineEnd: 'none' }}
    />
  )
}

export default SidebarNav
