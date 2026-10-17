# CM-04-02: Search Cases UI

## Goal

Allow clients to search their cases by title from the My Cases page.

## Page

`/cases`

## Acceptance Criteria

- Display a search input above the cases table.
- Call `GET /api/v1/cases?search={query}`.
- Debounce search input to avoid excessive API requests.
- Update the cases table with matching results.
- Reset pagination to the first page when searching.
- Show an empty state when no cases match.
- Clearing the search restores all cases.
- Show loading and error states.
- Support desktop and mobile layouts.

## Testing

- Client can search cases by partial title.
- Search results update after typing.
- Clearing the search restores all cases.
- No matching cases displays an empty state.
- Search resets pagination to the first page.
- API errors display an error message.

## Dependencies

- CM-01-02: My Cases Page
- CM-04-01: Search Cases API
