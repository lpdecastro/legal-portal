import type { ThemeConfig } from 'antd'

export const theme: ThemeConfig = {
  token: {
    colorPrimary: '#1f3a68',
    colorInfo: '#1f3a68',
    colorBgLayout: '#f5f7fa',
    borderRadius: 8,
    fontFamily:
      "system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
  },
  components: {
    Layout: {
      headerBg: '#ffffff',
      headerPadding: '0 24px',
      siderBg: '#ffffff',
    },
    Menu: {
      itemSelectedBg: '#e8eef8',
      itemSelectedColor: '#1f3a68',
      itemHoverBg: '#f0f3f8',
    },
  },
}
