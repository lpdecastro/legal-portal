# CASE-04: Delete Draft Case

## User Story

As a client, I want to delete an existing draft case, so that I can remove cases I no longer need.

## Acceptance Criteria

1. Client can delete their own draft case.
2. Delete action is available only for cases with `DRAFT` status.
3. Client sees a confirmation dialog before deleting.
4. Client can cancel the deletion without changing the case.
5. Client sees a success message after deletion.
6. Deleted cases are soft-deleted and no longer accessible.
7. Client sees an appropriate error message if deletion fails.
8. Client cannot delete another client's case.
9. Submitted and closed cases cannot be deleted.

## Tasks

| ID         | Task             | Type |
| ---------- | ---------------- | ---- |
| CASE-04-01 | Delete Draft API | BE   |
| CASE-04-02 | Delete Draft UI  | FE   |

## Dependencies

- Authenticated client session.
- CASE-02 – Create Draft Case.
- CASE-03-01 – Get Case Details API.

## Definition of Done

- All acceptance criteria are met.
- Client can successfully delete their own draft case.
- Backend enforces case ownership and draft-only deletion.
- Automated BE and FE tests pass.
