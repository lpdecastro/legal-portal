# CASE-11: Search Cases

## User Story

As a client, I want to search my cases by title so I can quickly find a specific case.

## Page

My Cases (`/cases`)

## Acceptance Criteria

1. Search cases by title.
2. Support partial, case-insensitive matching.
3. Show only matching cases belonging to the client.
4. Update results when the search query changes.
5. Show an empty state when no cases match.
6. Clearing the search restores all cases.
7. Combine search with the status filter.
8. Reset pagination to the first page when searching.

## Tasks

| ID         | Task             | Type |
| ---------- | ---------------- | ---- |
| CASE-11-01 | Search Cases API | BE   |
| CASE-11-02 | Search Cases UI  | FE   |

## Dependencies

- Authenticated client session.
- CASE-03 – View My Cases.
- CASE-10 – Filter Cases.

## Definition of Done

- All acceptance criteria are met.
- Automated BE and FE tests pass.
