# CASE-09-01: Paginate Cases API

**Type:** BE  
**Story:** CASE-09 – Paginate Cases

## Description

Allow authenticated clients to retrieve their cases with server-side pagination.

## API Contract

**GET** `/api/v1/cases?page=0&size=10`

**Query Parameters**

- `page` — Zero-based page index (default: `0`).
- `size` — Number of cases per page (default: `10`, max: `100`).

**Response: 200 OK**

```json
{
  "content": [
    {
      "id": "uuid",
      "title": "Property Dispute",
      "category": "Real Estate",
      "status": "SUBMITTED",
      "createdAt": "2026-10-15T08:00:00Z"
    }
  ],
  "page": {
    "size": 10,
    "number": 0,
    "totalElements": 25,
    "totalPages": 3
  }
}
```

**Errors**

- `400` — Invalid pagination parameters
- `401` — Unauthorized
- `500` — Internal server error

## Acceptance Criteria

1. Return only the requested page of the authenticated client's cases.
2. Use zero-based page indexing.
3. Sort by creation date descending by default, with a stable secondary sort by ID.
4. Return pagination metadata.
5. Enforce a maximum page size of `100`.
6. Return an empty `content` array for pages beyond the last page.
7. Reject negative page numbers and invalid page sizes.
8. Exclude soft-deleted cases.
9. Follow the standard API error response format.

## Testing

- Client can retrieve different pages of cases.
- Pagination metadata is correct.
- Cases appear in a consistent order across pages.
- Out-of-range pages return an empty list.
- Invalid pagination parameters return `400`.
- Other clients' cases are excluded.
- Unauthenticated requests return `401`.

## Dependencies

- CASE-03-01 – List My Cases API.
- Authentication and session management.

## Definition of Done

- All acceptance criteria are met.
- Automated BE tests pass.
