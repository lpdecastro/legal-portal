# CASE-08-02: Close Case UI

**Type:** FE  
**Story:** CASE-08 – Close Case

## Description

Allow clients to close a submitted case from the Case Details page.

## Page

`/cases/:id`

## Acceptance Criteria

1. Show Close Case button only for `SUBMITTED` cases.
2. Show a confirmation dialog before closing.
3. Call `POST /api/v1/cases/{id}/close` upon confirmation.
4. Disable the action while the request is processing.
5. Update status to `CLOSED` and display the closure date.
6. Show a success message after closing.
7. Show an error message if the request fails.
8. Hide Close Case button after successful closure.
9. Support desktop and mobile layouts.

## Testing

- Client can close a submitted case after confirmation.
- Canceling confirmation keeps the case unchanged.
- Closing displays updated status and closure date.
- Failed requests display an error without changing the status.
- Draft and closed cases do not show the Close Case button.

## Dependencies

- CASE-04-02 – Case Details Page.
- CASE-08-01 – Close Case API.
- Authentication and protected routes.

## Definition of Done

- All acceptance criteria are met.
- Automated FE tests pass.
