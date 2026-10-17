# CASE-10-01: Filter Cases API

**Type:** BE  
**Story:** CASE-10 – Filter Cases

## Description

Allow authenticated clients to filter their own cases by status.

## API Contract

**GET** `/api/v1/cases?status=SUBMITTED`

**Combined:** `GET /api/v1/cases?status=SUBMITTED&page=0&size=10`

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
  ]
}
```

**Errors**

- `400` — Invalid status
- `401` — Unauthorized
- `500` — Internal server error

## Acceptance Criteria

1. Filter by `DRAFT`, `SUBMITTED`, or `CLOSED`.
2. Apply the status filter before pagination; pagination metadata reflects the filtered results.
3. Return all statuses when `status` is omitted.
4. Return only cases belonging to the authenticated client.
5. Exclude soft-deleted cases.
6. Return an empty list when no cases match.
7. Reject invalid status values with `400`.
8. Follow the standard API error response format.

## Testing

- Client can filter cases by each status.
- Status filter works with pagination.
- Omitting status returns cases of all statuses.
- No matches return an empty list.
- Invalid status returns `400`.
- Other clients' cases are excluded.
- Unauthenticated requests return `401`.

## Dependencies

- CASE-03-01 – List My Cases API.
- CASE-09-01 – Paginate Cases API.
- Authentication and session management.

## Definition of Done

- All acceptance criteria are met.
- Automated BE tests pass.
