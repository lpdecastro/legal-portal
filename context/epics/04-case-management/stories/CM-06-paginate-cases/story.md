# CM-06: Paginate Cases

## User Story

As a client, I want to navigate through pages of my cases so I can browse them easily.

## Page

My Cases (`/cases`)

## Acceptance Criteria

- Display cases with server-side pagination.
- Allow clients to navigate between pages.
- Show current page, total pages, and total cases.
- Allow clients to change the page size.
- Preserve search and status filters when navigating pages.
- Reset to the first page when search, filter, or page size changes.
- Show an empty state when no cases exist.
- Show loading and error states.

## Tasks

- CM-06-01: Paginate Cases API
- CM-06-02: Pagination UI

## Dependencies

- CM-01: View My Cases
- CM-04: Search Cases
- CM-05: Filter Cases
