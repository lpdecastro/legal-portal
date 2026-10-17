# CASE-01-02: Legal Category Dropdown

**Type:** FE  
**Story:** CASE-01 – Legal Categories

## Description

Implement a reusable legal category dropdown component for the Create/Edit Draft forms using React, TypeScript, and Ant Design.

## Acceptance Criteria

1. Dropdown loads legal categories from `GET /api/v1/legal-categories`.
2. Displays categories alphabetically by name.
3. Client can select one legal category.
4. Selected category is reflected in the form.
5. Existing category is preselected when editing a draft.
6. Dropdown displays a loading state while fetching categories.
7. Dropdown displays an appropriate error message if the API fails.
8. Dropdown displays an empty state when no categories are available.
9. Client can retry loading categories after an API failure.

## Testing

- Verify categories are displayed in the dropdown.
- Verify selecting a category updates the form value.
- Verify an existing category is preselected.
- Verify loading state.
- Verify API error and retry behavior.
- Verify empty state when no categories are returned.

## Dependencies

- CASE-01-01 – List Legal Categories API.

## Definition of Done

- Reusable dropdown component is implemented.
- All acceptance criteria are met.
- Automated frontend tests pass.
