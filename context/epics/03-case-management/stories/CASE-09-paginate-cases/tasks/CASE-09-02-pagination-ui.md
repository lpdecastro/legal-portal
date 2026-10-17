# CASE-09-02: Pagination UI

**Type:** FE  
**Story:** CASE-09 – Paginate Cases

## Description

Allow clients to navigate through paginated cases on the My Cases page.

## Page

`/cases`

## Acceptance Criteria

1. Display pagination controls below the cases table.
2. Fetch cases using `GET /api/v1/cases?page={page}&size={size}`.
3. Show current page, total pages, and total cases.
4. Allow navigation between pages.
5. Allow changing the page size (10, 20, 50).
6. Reset to the first page when page size changes.
7. Disable navigation when no previous or next page exists.
8. Show loading and error states.
9. Support desktop and mobile layouts.

## Testing

- Client can navigate between pages.
- Changing page size updates the displayed cases.
- Changing page size resets pagination.
- Pagination controls reflect API metadata.
- API errors display an error message.

## Dependencies

- CASE-03-02 – My Cases Page.
- CASE-09-01 – Paginate Cases API.
- Authentication and protected routes.

## Definition of Done

- All acceptance criteria are met.
- Automated FE tests pass.
