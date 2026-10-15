# PROF-01-03: View and Edit Profile UI

## Goal

Allow clients to view and update their profile through a single page.

## Acceptance Criteria

- Display first name, last name, email, and phone number from the API.
- Allow editing of first name, last name, and phone number.
- Email is read-only.
- Provide Save and Cancel actions.
- Validate required fields and input lengths.
- Show loading, success, and error states.
- Prevent duplicate submissions.
- Saved changes persist after page refresh.
- Automated tests cover viewing, editing, validation, and API errors.

## API Integration

- `GET /api/v1/clients/me` — Load profile.
- `PATCH /api/v1/clients/me` — Save changes.
