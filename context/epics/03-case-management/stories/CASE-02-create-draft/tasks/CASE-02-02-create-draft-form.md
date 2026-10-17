# CASE-02-02: Create Draft Form

**Type:** FE  
**Story:** CASE-02 – Create Draft Case

## Description

Implement a Create Draft form using React, TypeScript, and Ant Design, allowing authenticated clients to save a new legal case as a draft.

## Acceptance Criteria

1. Client can access the Create Draft page.
2. Form includes:
   - Title – Required, maximum 200 characters.
   - Legal Category – Required, using the CASE-01 dropdown.
   - Description – Optional.
   - Location – Optional, maximum 255 characters.
3. Client can save a draft using the "Save Draft" button.
4. Form validates required fields and field length limits before saving.
5. Save Draft calls `POST /api/v1/cases` with form values.
6. Save button is disabled and displays a loading indicator while saving.
7. Successful creation displays a success message and navigates to the created case details page.
8. API validation errors are displayed against the relevant fields.
9. API failures display an appropriate error message without clearing the form.
10. Unauthenticated clients are redirected to login.

## Testing

- Verify form fields render correctly.
- Verify legal categories load in the dropdown.
- Verify required field validation.
- Verify maximum field length validation.
- Verify successful draft creation and navigation.
- Verify loading state prevents duplicate submissions.
- Verify API validation errors are displayed.
- Verify API failure preserves entered values.

## Dependencies

- CASE-01-02 – Legal Category Dropdown.
- CASE-02-01 – Create Draft API.
- Authentication and protected routes.

## Definition of Done

- All acceptance criteria are met.
- Client can successfully create a draft case.
- Automated frontend tests pass.
