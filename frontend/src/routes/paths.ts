export const paths = {
  login: '/login',
  register: '/register',
  forgotPassword: '/forgot-password',
  resetPassword: '/reset-password',
  verifyEmail: '/verify-email',
  dashboard: '/dashboard',
  cases: '/cases',
  createCase: '/cases/new',
  caseDetails: '/cases/:id',
  editCase: '/cases/:id/edit',
  profile: '/profile',
  settings: '/settings',
} as const;

export const caseDetailsPath = (id: string): string => `${paths.cases}/${id}`;

export const editCasePath = (id: string): string => `${caseDetailsPath(id)}/edit`;
