# CASE-05-02: Edit Draft Form

**Type:** FE  
**Story:** CASE-05 – Edit Draft Case

## Description

Implement an Edit Draft form using React, TypeScript, and Ant Design, allowing authenticated clients to update their existing draft cases.

Reuse the Create Draft form component where possible.

## Acceptance Criteria

1. Client can access the Edit Draft page for their own draft case.
2. Form loads case details from `GET /api/v1/cases/{id}`.
3. Form displays existing values:
   - Title – Required, maximum 200 characters.
   - Legal Category – Required, using the CASE-01 dropdown.
   - Description – Optional.
   - Location – Optional, maximum 255 characters.
4. Client can modify and save draft information.
5. Form validates required fields and field length limits before saving.
6. Save Changes calls `PATCH /api/v1/cases/{id}` with modified fields.
7. Save button is disabled and displays a loading indicator while saving.
8. Successful update displays a success message and navigates to the case details page.
9. API validation errors are displayed against the relevant fields.
10. API failures display an appropriate error message without clearing the form.
11. Client sees a loading indicator while retrieving case details.
12. Client sees an appropriate error message if loading fails.
13. Submitted and closed cases cannot be edited.
14. Unauthenticated clients are redirected to login.
15. Missing cases or cases belonging to another client display a not-found state.
16. Saving with no changes skips the API call, shows "No changes to save." and navigates to the case details page.
17. Malformed case IDs (400) display the same not-found state.

## Testing

- Verify existing case details populate the form.
- Verify legal category is preselected.
- Verify fields can be modified and saved.
- Verify required field and maximum length validation.
- Verify successful update and navigation.
- Verify loading states for fetching and saving.
- Verify API validation errors are displayed.
- Verify API failures preserve entered values.
- Verify submitted and closed cases cannot be edited.
- Verify missing or unauthorized cases display a not-found state.
- Verify saving with no changes sends no request.
- Verify unauthenticated clients are redirected to login.

## Dependencies

- CASE-01-02 – Legal Category Dropdown.
- CASE-04-01 – Get Case Details API.
- CASE-05-01 – Update Draft API.
- CASE-02-02 – Create Draft Form.
- Authentication and protected routes.

## Definition of Done

- All acceptance criteria are met.
- Client can successfully load and update their own draft case.
- Automated frontend tests pass.
