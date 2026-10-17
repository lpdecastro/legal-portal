# CASE-10: Filter Cases

## User Story

As a client, I want to filter my cases by status so I can quickly find cases in a specific state.

## Page

My Cases (`/cases`)

## Acceptance Criteria

1. Filter cases by `DRAFT`, `SUBMITTED`, or `CLOSED` status.
2. Show only matching cases belonging to the client.
3. Support an All Statuses option.
4. Preserve the filter when navigating pages.
5. Reset pagination to the first page when the filter changes.
6. Show an empty state when no cases match.
7. Clearing the filter restores all cases.

## Tasks

| ID         | Task             | Type |
| ---------- | ---------------- | ---- |
| CASE-10-01 | Filter Cases API | BE   |
| CASE-10-02 | Filter Cases UI  | FE   |

## Dependencies

- Authenticated client session.
- CASE-03 – View My Cases.
- CASE-09 – Paginate Cases.

## Definition of Done

- All acceptance criteria are met.
- Automated BE and FE tests pass.
