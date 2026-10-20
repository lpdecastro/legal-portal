# BKL-05: Sort Cases

**Type:** FS  
**Epic:** BKL – Backlog

## Objective

Let clients sort the My Cases list by column.

## Acceptance Criteria

1. `GET /api/v1/cases` accepts `sort=<field>,<asc|desc>` for `title`, `category`, `status` and `createdAt`.
2. Default stays `createdAt,desc`; ID remains the stable secondary sort.
3. Unknown sort fields or directions return 400 `VALIDATION_ERROR`.
4. Sorting applies across all pages, not just the current one.
5. My Cases table columns are sortable; changing the sort resets to the first page.
6. Sort works together with pagination, filters and search.

## Dependencies

- CASE-09-01, CASE-09-02.
