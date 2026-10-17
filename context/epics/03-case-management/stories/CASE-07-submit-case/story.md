# CASE-07: Submit Case

## User Story

As a client, I want to submit my draft case, so that it can be finalized for legal assistance.

## Acceptance Criteria

1. Client can submit their own draft case.
2. Submit action is available only for cases with `DRAFT` status.
3. Case must have a title, active legal category, description, and location before submission.
4. Client sees validation errors if required information is missing or invalid.
5. Client sees a confirmation dialog before submitting.
6. Client can cancel submission without changing the case.
7. Client sees a success message after submission.
8. Successfully submitted cases have `SUBMITTED` status.
9. Submitted cases can no longer be edited, deleted, or submitted again.
10. Client sees an appropriate error message if submission fails.
11. Client cannot submit another client's case.

## Tasks

| ID         | Task            | Type |
| ---------- | --------------- | ---- |
| CASE-07-01 | Submit Case API | BE   |
| CASE-07-02 | Submit Case UI  | FE   |

## Dependencies

- Authenticated client session.
- CASE-01 – Legal Categories.
- CASE-02 – Create Draft Case.
- CASE-04 – View Case Details.
- CASE-05 – Edit Draft Case.

## Definition of Done

- All acceptance criteria are met.
- Client can successfully submit their own valid draft case.
- Backend enforces ownership, required fields, and draft-only submission.
- Automated BE and FE tests pass.
