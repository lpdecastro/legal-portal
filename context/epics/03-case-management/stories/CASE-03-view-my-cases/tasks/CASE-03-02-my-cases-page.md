# CASE-03-02: My Cases Page

**Type:** FE  
**Story:** CASE-03 – View My Cases

## Description

Allow clients to view their cases in a table.

## Page

`/cases`

## Acceptance Criteria

1. Fetch cases using `GET /api/v1/cases`.
2. Display case title, category, status, and creation date.
3. Clicking a case navigates to `/cases/:id`.
4. Show an empty state when no cases exist.
5. Show a loading indicator while fetching cases.
6. Show an error message with a retry option when fetching fails.
7. Support desktop and mobile layouts.

## Testing

- Client can view their cases.
- Clicking a case opens its details page.
- Empty, loading, and error states display correctly.
- Retry reloads the cases after an error.

## Dependencies

- Authentication and protected routes.

## Definition of Done

- All acceptance criteria are met.
- Automated FE tests pass.
