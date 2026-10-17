# CASE-04-02: Delete Draft UI

**Type:** FE  
**Story:** CASE-04 – Delete Draft Case

## Description

Implement a Delete Draft action using React, TypeScript, and Ant Design, allowing authenticated clients to delete their existing draft cases after confirmation.

## Acceptance Criteria

1. Client can delete their own draft case from the Case Details page.
2. Delete action is available only for cases with `DRAFT` status.
3. Clicking Delete displays a confirmation dialog.
4. Confirmation dialog provides Cancel and Delete options.
5. Cancel closes the dialog without deleting the case.
6. Confirming deletion calls `DELETE /api/v1/cases/{id}`.
7. Delete button displays a loading indicator and prevents duplicate requests while deleting.
8. Successful deletion displays a success message and navigates to My Cases.
9. API failures display an appropriate error message without navigating away.
10. If the case was already deleted or is inaccessible, display an appropriate error message.
11. Unauthenticated clients are redirected to login.

## Testing

- Verify Delete action is available for draft cases.
- Verify Delete action is unavailable for submitted and closed cases.
- Verify confirmation dialog appears.
- Verify Cancel does not trigger deletion.
- Verify confirming deletion calls the correct API.
- Verify loading state prevents duplicate requests.
- Verify successful deletion and navigation.
- Verify API failure displays an error message.
- Verify unauthenticated clients are redirected to login.

## Dependencies

- CASE-04-01 – Delete Draft API.
- CASE-03-01 – Get Case Details API.
- Case Details page.
- Authentication and protected routes.

## Definition of Done

- All acceptance criteria are met.
- Client can successfully delete their own draft case after confirmation.
- Automated frontend tests pass.
