# CM-06-02: Pagination UI

## Goal

Allow clients to navigate through paginated cases on the My Cases page.

## Page

`/cases`

## Acceptance Criteria

- Display pagination controls below the cases table.
- Fetch cases using `GET /api/v1/cases?page={page}&size={size}`.
- Show current page, total pages, and total cases.
- Allow navigation between pages.
- Allow changing the page size (10, 20, 50).
- Preserve search and status filters when changing pages.
- Reset to the first page when search, filter, or page size changes.
- Disable navigation when no previous or next page exists.
- Show loading and error states.
- Support desktop and mobile layouts.

## Testing

- Client can navigate between pages.
- Changing page size updates the displayed cases.
- Search and status filters persist across pages.
- Changing search, filter, or page size resets pagination.
- Pagination controls reflect API metadata.
- API errors display an error message.

## Dependencies

- CM-01-02: My Cases Page
- CM-04-02: Search Cases UI
- CM-05-02: Filter Cases UI
- CM-06-01: Paginate Cases API
