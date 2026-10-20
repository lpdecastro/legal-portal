const dateFormat = new Intl.DateTimeFormat('en-US', { dateStyle: 'medium' })

// Formats an ISO timestamp as a date like "Oct 18, 2026".
export const formatDate = (iso: string): string => dateFormat.format(new Date(iso))
