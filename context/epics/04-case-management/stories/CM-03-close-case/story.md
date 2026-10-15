# CM-03: Close Case

## User Story

As a client, I want to close a submitted case when I no longer need assistance.

## Page

Case Details (`/cases/:id`)

## Acceptance Criteria

- Show Close Case action only for submitted cases.
- Require confirmation before closing a case.
- Update case status to `CLOSED` after confirmation.
- Display the updated status and closure date.
- Show a success message after closing.
- Show an error message if closing fails.
- Closed cases cannot be reopened.
- Clients can only close their own cases.

## Tasks

- CM-03-01: Close Case API
- CM-03-02: Close Case UI

## Dependencies

- CM-02: View Case Details
