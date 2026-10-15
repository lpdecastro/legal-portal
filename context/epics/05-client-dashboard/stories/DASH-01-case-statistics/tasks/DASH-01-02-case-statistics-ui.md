# DASH-01-02: Case Statistics UI

## Task

Display case statistics on the client dashboard using the Dashboard Summary API.

## Acceptance Criteria

- Display total, draft, submitted, and closed case counts.
- Fetch data from `GET /api/v1/dashboard/summary` using TanStack Query.
- Display zero for statuses with no cases.
- Show loading and error states.
- Ensure statistics are responsive on desktop and mobile.
- Share the dashboard API response with the Recent Cases UI to avoid duplicate requests.
