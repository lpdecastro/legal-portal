# CM-03-02: Close Case UI

## Goal

Allow clients to close a submitted case from the Case Details page.

## Page

`/cases/:id`

## Acceptance Criteria

- Show Close Case button only for `SUBMITTED` cases.
- Show a confirmation dialog before closing.
- Call `POST /api/v1/cases/{id}/close` upon confirmation.
- Disable the action while the request is processing.
- Update status to `CLOSED` and display the closure date.
- Show a success message after closing.
- Show an error message if the request fails.
- Hide Close Case button after successful closure.
- Support desktop and mobile layouts.

## Testing

- Client can close a submitted case after confirmation.
- Canceling confirmation keeps the case unchanged.
- Closing displays updated status and closure date.
- Failed requests display an error without changing the status.
- Draft and closed cases do not show the Close Case button.

## Dependency

- CM-03-01: Close Case API
