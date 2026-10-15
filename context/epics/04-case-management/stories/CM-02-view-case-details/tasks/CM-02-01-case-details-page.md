# CM-02-01: Case Details Page

## Goal

Allow clients to view their case information and status.

## Page

`/cases/:id`

## Acceptance Criteria

- Fetch case details using `GET /api/v1/cases/:id`.
- Display title, category, description, location, status, and relevant dates.
- Show actions based on status:
  - `DRAFT` — Edit, Delete, Submit
  - `SUBMITTED` — Close
  - `CLOSED` — No actions
- Navigate to the edit page when clicking Edit.
- Show loading and error states.
- Show a not-found message for missing or inaccessible cases.
- Support desktop and mobile layouts.

## Testing

- Client can view their case details.
- Available actions match the case status.
- Edit navigates to `/cases/:id/edit`.
- Missing or inaccessible cases show a not-found message.
- Loading and error states display correctly.

## Dependency

- CASE-03-01: Get Case Details API
