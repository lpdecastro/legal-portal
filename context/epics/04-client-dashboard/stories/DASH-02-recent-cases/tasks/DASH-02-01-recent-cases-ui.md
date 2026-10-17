# DASH-02-01: Recent Cases UI

## Task

Display recent cases on the client dashboard using the Dashboard Summary API.

## Acceptance Criteria

- Display the 5 most recent cases, newest first.
- Show case title, status, and creation date.
- Navigate to `/cases/:id` when a case is clicked.
- Display an empty state when no cases exist.
- Show loading and error states.
- Ensure the UI is responsive on desktop and mobile.
- Reuse the dashboard summary data from `DASH-01-02` to avoid duplicate API requests.
