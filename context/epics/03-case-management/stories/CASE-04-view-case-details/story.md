# CASE-04: View Case Details

## User Story

As a client, I want to view my case details so I can review its information and status.

## Page

Case Details (`/cases/:id`)

## Acceptance Criteria

1. Display case title, category, description, location, status, and dates.
2. Only allow clients to view their own cases.
3. Show available actions based on case status.
4. Show loading and error states.
5. Show a not-found message for missing or inaccessible cases.

## Tasks

| ID         | Task                 | Type |
| ---------- | -------------------- | ---- |
| CASE-04-01 | Get Case Details API | BE   |
| CASE-04-02 | Case Details Page    | FE   |

## Dependencies

- Authenticated client session.
- CASE-02 – Create Draft.

## Definition of Done

- All acceptance criteria are met.
- Automated BE and FE tests pass.
