# CM-01-02: My Cases Page

## Goal

Allow clients to view their cases in a table.

## Page

`/cases`

## Acceptance Criteria

- Fetch cases using `GET /api/v1/cases`.
- Display case title, category, status, and creation date.
- Clicking a case navigates to `/cases/:id`.
- Show an empty state when no cases exist.
- Show a loading indicator while fetching cases.
- Show an error message with a retry option when fetching fails.
- Support desktop and mobile layouts.

## Testing

- Client can view their cases.
- Clicking a case opens its details page.
- Empty, loading, and error states display correctly.
- Retry reloads the cases after an error.
