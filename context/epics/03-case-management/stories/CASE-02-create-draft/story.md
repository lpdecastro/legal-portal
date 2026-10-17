# CASE-02: Create Draft Case

## User Story

As a client, I want to create and save a draft case, so that I can complete and submit it later.

## Acceptance Criteria

1. Client can access the Create Draft form.
2. Client can enter a title, legal category, description, and location.
3. Client can save a case as a draft without completing all fields.
4. Client must provide a title and legal category to save a draft.
5. Newly created cases have `DRAFT` status.
6. Client sees a success message after saving.
7. Client sees validation errors for invalid inputs.
8. Client sees an appropriate error message if saving fails.
9. Client cannot accidentally create duplicate drafts by clicking Save multiple times.

## Tasks

| ID         | Task              | Type |
| ---------- | ----------------- | ---- |
| CASE-02-01 | Create Draft API  | BE   |
| CASE-02-02 | Create Draft Form | FE   |

## Dependencies

- Authenticated client session.
- CASE-01 – Legal Categories.

## Definition of Done

- All acceptance criteria are met.
- Client can successfully create a draft case.
- Draft is associated with the authenticated client.
- Automated BE and FE tests pass.
