# CASE-03: View My Cases

## User Story

As a client, I want to view all my cases so I can track their status.

## Page

My Cases (`/cases`)

## Acceptance Criteria

1. Display only cases belonging to the logged-in client.
2. Show case title, category, status, and creation date.
3. Clicking a case opens its details page.
4. Show an empty state when no cases exist.
5. Show loading and error states.

## Tasks

| ID         | Task              | Type |
| ---------- | ----------------- | ---- |
| CASE-03-01 | List My Cases API | BE   |
| CASE-03-02 | My Cases Page     | FE   |

## Dependencies

- Authenticated client session.

## Definition of Done

- All acceptance criteria are met.
- Automated BE and FE tests pass.
