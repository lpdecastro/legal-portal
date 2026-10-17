# CASE-08: Close Case

## User Story

As a client, I want to close a submitted case when I no longer need assistance.

## Page

Case Details (`/cases/:id`)

## Acceptance Criteria

1. Show Close Case action only for submitted cases.
2. Require confirmation before closing a case.
3. Update case status to `CLOSED` after confirmation.
4. Display the updated status and closure date.
5. Show a success message after closing.
6. Show an error message if closing fails.
7. Closed cases cannot be reopened.
8. Clients can only close their own cases.

## Tasks

| ID         | Task           | Type |
| ---------- | -------------- | ---- |
| CASE-08-01 | Close Case API | BE   |
| CASE-08-02 | Close Case UI  | FE   |

## Dependencies

- Authenticated client session.
- CASE-04 – View Case Details.

## Definition of Done

- All acceptance criteria are met.
- Automated BE and FE tests pass.
