# BKL-04: Cancel Edit Draft

**Type:** FE  
**Epic:** BKL – Backlog

## Objective

Let clients leave the Edit Draft form without saving.

## Acceptance Criteria

1. Edit Draft form shows a Cancel button next to Save Changes.
2. Cancel navigates to the case details page (`/cases/:id`) without sending a request.
3. Unsaved changes are discarded.
4. Cancel is disabled while saving.

## Dependencies

- CASE-04-02, CASE-05-02.
