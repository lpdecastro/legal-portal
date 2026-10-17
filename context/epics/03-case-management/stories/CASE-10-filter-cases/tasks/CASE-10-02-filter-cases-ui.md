# CASE-10-02: Filter Cases UI

**Type:** FE  
**Story:** CASE-10 – Filter Cases

## Description

Allow clients to filter their cases by status from the My Cases page.

## Page

`/cases`

## Acceptance Criteria

1. Display a status dropdown above the cases table.
2. Provide `All Statuses`, `DRAFT`, `SUBMITTED`, and `CLOSED` options.
3. Call `GET /api/v1/cases?status={status}`.
4. Preserve the status filter when changing pages.
5. Update the cases table when the filter changes.
6. Reset pagination to the first page when filtering.
7. Selecting All Statuses removes the status filter.
8. Show an empty state when no cases match.
9. Show loading and error states.
10. Support desktop and mobile layouts.

## Testing

- Client can filter cases by each status.
- Status filter persists across pages.
- Selecting All Statuses restores unfiltered results.
- Changing the filter resets pagination to the first page.
- No matching cases displays an empty state.
- API errors display an error message.

## Dependencies

- CASE-03-02 – My Cases Page.
- CASE-09-02 – Pagination UI.
- CASE-10-01 – Filter Cases API.
- Authentication and protected routes.

## Definition of Done

- All acceptance criteria are met.
- Automated FE tests pass.
