# CASE-03: Edit Draft Case

## User Story

As a client, I want to view and edit an existing draft case, so that I can update its information before submission.

## Acceptance Criteria

1. Client can open an existing draft case for editing.
2. Edit form displays the saved title, legal category, description, and location.
3. Client can modify and save draft information.
4. Title and legal category are required.
5. Client sees validation errors for invalid inputs.
6. Client sees a success message after saving changes.
7. Client sees an appropriate error message if loading or saving fails.
8. Only cases with `DRAFT` status can be edited.
9. Client cannot access or edit another client's case.
10. Submitted and closed cases cannot be edited.

## Tasks

| ID         | Task                 | Type |
| ---------- | -------------------- | ---- |
| CASE-03-01 | Get Case Details API | BE   |
| CASE-03-02 | Update Draft API     | BE   |
| CASE-03-03 | Edit Draft Form      | FE   |

## Dependencies

- Authenticated client session.
- CASE-01 – Legal Categories.
- CASE-02 – Create Draft Case.

## Definition of Done

- All acceptance criteria are met.
- Client can load and update their own draft case.
- Backend enforces case ownership and draft-only editing.
- Automated BE and FE tests pass.
