# CASE-05-02: Submit Case UI

**Type:** FE  
**Story:** CASE-05 – Submit Case

## Description

Implement a Submit Case action using React, TypeScript, and Ant Design, allowing authenticated clients to submit their existing draft cases after validation and confirmation.

## Acceptance Criteria

1. Client can submit their own draft case from the Case Details page.
2. Submit action is available only for cases with `DRAFT` status.
3. Clicking Submit validates the required case information:
   - Title – Required, maximum 200 characters.
   - Legal Category – Required and active.
   - Description – Required, non-blank.
   - Location – Required, non-blank, maximum 255 characters.
4. If required information is missing or invalid, client sees an appropriate validation message and can navigate to Edit Draft.
5. Valid cases display a confirmation dialog before submission.
6. Confirmation dialog provides Cancel and Submit options.
7. Cancel closes the dialog without submitting the case.
8. Confirming submission calls `POST /api/v1/cases/{id}/submit`.
9. Submit button displays a loading indicator and prevents duplicate requests while submitting.
10. Successful submission displays a success message and updates the Case Details page with `SUBMITTED` status.
11. Submit, Edit, and Delete actions are no longer available after successful submission.
12. API validation errors are displayed with an option to edit the draft.
13. API failures display an appropriate error message without navigating away.
14. If the case was already submitted, closed, deleted, or is inaccessible, the UI displays an appropriate message and refreshes the case information when applicable.
15. Unauthenticated clients are redirected to login.

## Testing

- Verify Submit action is available only for draft cases.
- Verify missing or invalid required information displays validation errors.
- Verify valid cases display a confirmation dialog.
- Verify Cancel does not trigger submission.
- Verify confirming submission calls the correct API.
- Verify loading state prevents duplicate requests.
- Verify successful submission updates the status and available actions.
- Verify API validation errors provide an option to edit the draft.
- Verify API failures display an appropriate error message.
- Verify submitted and closed cases cannot be submitted again.
- Verify unauthenticated clients are redirected to login.

## Dependencies

- CASE-05-01 – Submit Case API.
- CASE-03-01 – Get Case Details API.
- CASE-03-03 – Edit Draft Form.
- Case Details page.
- Authentication and protected routes.

## Definition of Done

- All acceptance criteria are met.
- Client can successfully submit their own valid draft case after confirmation.
- Case Details page reflects the updated submission status.
- Automated frontend tests pass.
