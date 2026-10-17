# Epic: Case Management

**ID:** CASE  
**Goal:** Allow authenticated clients to create, view, manage, submit, and close their own legal cases.

## Pages

| Page         | Route             | Contents                                                    |
| ------------ | ----------------- | ----------------------------------------------------------- |
| My Cases     | `/cases`          | Case list, pagination, status filter, search.               |
| Create Case  | `/cases/new`      | Case form, Save Draft button.                               |
| Case Details | `/cases/:id`      | Case info; Edit, Delete, Submit (draft); Close (submitted). |
| Edit Draft   | `/cases/:id/edit` | Case form, Save Changes button.                             |

## Business Rules

- Only authenticated clients can access cases.
- Each case belongs to one client; clients can only access their own cases.
- New cases have `DRAFT` status.
- Only drafts can be edited or soft-deleted.
- Only active legal categories can be selected.
- Submission requires title, category, description, and location.
- Submitted cases cannot be edited, deleted, or submitted again.
- Submitted cases can be closed; closed cases cannot be reopened.

## Stories

| ID      | Story             | Description                                    |
| ------- | ----------------- | ---------------------------------------------- |
| CASE-01 | Legal Categories  | Display active legal categories in a dropdown. |
| CASE-02 | Create Draft      | Create a new case with DRAFT status.           |
| CASE-03 | View My Cases     | List the client's cases.                       |
| CASE-04 | View Case Details | Display a single case.                         |
| CASE-05 | Edit Draft        | Update an existing draft case.                 |
| CASE-06 | Delete Draft      | Soft-delete an existing draft case.            |
| CASE-07 | Submit Case       | Validate and submit a draft case.              |
| CASE-08 | Close Case        | Close a submitted case.                        |
| CASE-09 | Paginate Cases    | Paginate the case list.                        |
| CASE-10 | Filter Cases      | Filter the case list.                          |
| CASE-11 | Search Cases      | Search the case list.                          |

## Dependencies

- Authentication and session management.
- Authenticated application layout.
- PostgreSQL database.

## Definition of Done

- All stories are completed.
- Business rules and authorization are enforced by the backend.
- Frontend handles loading, validation, success, and error states.
- Automated FE and BE tests pass.
