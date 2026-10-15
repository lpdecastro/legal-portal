# CM-05-02: Filter Cases UI

## Goal

Allow clients to filter their cases by status from the My Cases page.

## Page

`/cases`

## Acceptance Criteria

- Display a status dropdown above the cases table.
- Provide `All Statuses`, `DRAFT`, `SUBMITTED`, and `CLOSED` options.
- Call `GET /api/v1/cases?status={status}`.
- Combine status filtering with the existing search query.
- Update the cases table when the filter changes.
- Reset pagination to the first page when filtering.
- Selecting All Statuses removes the status filter.
- Show an empty state when no cases match.
- Show loading and error states.
- Support desktop and mobile layouts.

## Testing

- Client can filter cases by each status.
- Search and status filters work together.
- Selecting All Statuses restores unfiltered results.
- Changing the filter resets pagination to the first page.
- No matching cases displays an empty state.
- API errors display an error message.

## Dependencies

- CM-01-02: My Cases Page
- CM-04-02: Search Cases UI
- CM-05-01: Filter Cases API
