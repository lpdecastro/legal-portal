# CASE-09: Paginate Cases

## User Story

As a client, I want to navigate through pages of my cases so I can browse them easily.

## Page

My Cases (`/cases`)

## Acceptance Criteria

1. Display cases with server-side pagination.
2. Allow clients to navigate between pages.
3. Show current page, total pages, and total cases.
4. Allow clients to change the page size.
5. Reset to the first page when page size changes.
6. Show an empty state when no cases exist.
7. Show loading and error states.

## Tasks

| ID         | Task               | Type |
| ---------- | ------------------ | ---- |
| CASE-09-01 | Paginate Cases API | BE   |
| CASE-09-02 | Pagination UI      | FE   |

## Dependencies

- Authenticated client session.
- CASE-03 – View My Cases.

## Definition of Done

- All acceptance criteria are met.
- Automated BE and FE tests pass.
