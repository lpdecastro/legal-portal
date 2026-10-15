# CASE-01: Legal Categories

## User Story

As a client, I want to select a legal category when creating or editing a draft case, so that my case is classified correctly.

## Acceptance Criteria

1. Client can view available legal categories in a dropdown.
2. Dropdown displays only active legal categories.
3. Client can select one legal category.
4. Previously selected category is displayed when editing a draft.
5. Client sees an appropriate message if categories cannot be loaded.
6. Client sees an empty state if no active categories are available.

## Tasks

| ID         | Task                      | Type |
| ---------- | ------------------------- | ---- |
| CASE-01-01 | List Legal Categories API | BE   |
| CASE-01-02 | Legal Category Dropdown   | FE   |

## Dependencies

- Authenticated client session.

## Definition of Done

- All acceptance criteria are met.
- BE and FE automated tests pass.
- Dropdown works in both Create Draft and Edit Draft forms.
