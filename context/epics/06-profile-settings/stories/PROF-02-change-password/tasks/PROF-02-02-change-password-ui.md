# PROF-02-02: Change Password UI

## Goal

Allow authenticated clients to securely change their password through the Profile & Settings page.

## Acceptance Criteria

- Display current password, new password, and confirm password fields.
- Require all fields.
- Validate minimum 12 characters and matching new passwords.
- Provide show/hide password controls.
- Display validation and API errors.
- Show success confirmation and clear password fields after a successful change.
- Prevent duplicate submissions.
- Never persist passwords in browser storage.
- Automated tests cover successful changes, validation, and API errors.

## API Integration

- `PATCH /api/v1/clients/me/password` — Change password.
