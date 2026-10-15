# Epic: Case Submission

**ID:** CASE  
**Goal:** Allow authenticated clients to create, manage, and submit legal cases.

## Scope

- Browse active legal categories through a dropdown.
- Create a draft case.
- View and edit an existing draft case.
- Delete a draft case.
- Submit a draft case.

## Business Rules

- Only authenticated clients can create cases.
- Each case belongs to one client.
- Clients can only access their own cases.
- New cases have `DRAFT` status.
- Draft cases can be edited or soft-deleted.
- Submission requires title, category, description, and location.
- Only active legal categories can be selected.
- Submitted cases cannot be edited or deleted.
- Submitted cases cannot be submitted again.

## Stories

| ID      | Story            | Description                                    |
| ------- | ---------------- | ---------------------------------------------- |
| CASE-01 | Legal Categories | Display active legal categories in a dropdown. |
| CASE-02 | Create Draft     | Create a new case with DRAFT status.           |
| CASE-03 | Edit Draft       | Load and update an existing draft case.        |
| CASE-04 | Delete Draft     | Soft-delete an existing draft case.            |
| CASE-05 | Submit Case      | Validate and submit a draft case.              |

## Dependencies

- Authentication and session management.
- Authenticated application layout.
- PostgreSQL database.

## Definition of Done

- All five stories are completed.
- Clients can create, edit, delete, and submit their own drafts.
- Business rules and authorization are enforced by the backend.
- Frontend handles loading, validation, success, and error states.
- Automated FE and BE tests pass.
- End-to-end case submission flow passes.
