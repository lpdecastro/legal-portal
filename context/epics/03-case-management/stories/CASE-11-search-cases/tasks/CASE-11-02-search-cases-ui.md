# CASE-11-02: Search Cases UI

**Type:** FE  
**Story:** CASE-11 – Search Cases

## Description

Allow clients to search their cases by title from the My Cases page.

## Page

`/cases`

## Acceptance Criteria

1. Display a search input above the cases table.
2. Call `GET /api/v1/cases?search={query}`.
3. Debounce search input to avoid excessive API requests.
4. Combine search with the selected status filter.
5. Preserve the search query when changing pages.
6. Update the cases table with matching results.
7. Reset pagination to the first page when searching.
8. Show an empty state when no cases match.
9. Clearing the search restores all cases.
10. Show loading and error states.
11. Support desktop and mobile layouts.

## Testing

- Client can search cases by partial title.
- Search results update after typing.
- Clearing the search restores all cases.
- Search and status filters work together and persist across pages.
- No matching cases displays an empty state.
- Search resets pagination to the first page.
- API errors display an error message.

## Dependencies

- CASE-03-02 – My Cases Page.
- CASE-10-02 – Filter Cases UI.
- CASE-11-01 – Search Cases API.
- Authentication and protected routes.

## Definition of Done

- All acceptance criteria are met.
- Automated FE tests pass.
