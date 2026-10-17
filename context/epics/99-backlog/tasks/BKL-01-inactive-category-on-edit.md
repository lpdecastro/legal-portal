# BKL-01: Inactive Category on Edit Draft

**Type:** FS  
**Epic:** BKL – Backlog

## Description

A draft whose legal category was deactivated can't be saved or submitted with it (400), but the Edit Draft form preselects it and the dropdown shows the raw ID.

## Acceptance Criteria

1. Get Case Details returns the category name and whether it's active.
2. Edit Draft form leaves an inactive category unselected and warns "<name> is no longer available. Please choose another category."
3. `LegalCategorySelect` shows the placeholder, not the raw ID, for a value not in the active list.

## Dependencies

- CASE-04-01, CASE-05-02.
