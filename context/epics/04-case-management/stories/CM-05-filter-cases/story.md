# CM-05: Filter Cases

## User Story

As a client, I want to filter my cases by status so I can quickly find cases in a specific state.

## Page

My Cases (`/cases`)

## Acceptance Criteria

- Filter cases by `DRAFT`, `SUBMITTED`, or `CLOSED` status.
- Show only matching cases belonging to the client.
- Support an All Statuses option.
- Combine status filtering with search.
- Reset pagination to the first page when the filter changes.
- Show an empty state when no cases match.
- Clearing the filter restores all cases.

## Tasks

- CM-05-01: Filter Cases API
- CM-05-02: Filter Cases UI

## Dependencies

- CM-01: View My Cases
- CM-04: Search Cases
