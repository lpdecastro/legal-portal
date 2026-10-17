# CASE-04-02: Case Details Page

**Type:** FE  
**Story:** CASE-04 – View Case Details

## Description

Allow clients to view their case information and status.

## Page

`/cases/:id`

## Acceptance Criteria

1. Fetch case details using `GET /api/v1/cases/:id`.
2. Display title, category, description, location, status, and relevant dates.
3. Show actions based on status:
   - `DRAFT` — Edit, Delete, Submit
   - `SUBMITTED` — Close
   - `CLOSED` — No actions
4. Navigate to the edit page when clicking Edit.
5. Show loading and error states.
6. Show a not-found message for missing or inaccessible cases.
7. Support desktop and mobile layouts.

## Testing

- Client can view their case details.
- Available actions match the case status.
- Edit navigates to `/cases/:id/edit`.
- Missing or inaccessible cases show a not-found message.
- Loading and error states display correctly.

## Dependencies

- CASE-04-01 – Get Case Details API.
- Authentication and protected routes.

## Definition of Done

- All acceptance criteria are met.
- Automated FE tests pass.
