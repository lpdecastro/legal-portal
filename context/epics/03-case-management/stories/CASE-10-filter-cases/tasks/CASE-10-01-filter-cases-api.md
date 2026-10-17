# CM-05-01: Filter Cases API

## Goal

Allow authenticated clients to filter their own cases by status.

## API Contract

**GET** `/api/v1/cases?status=SUBMITTED`

**Combined Search:** `GET /api/v1/cases?search=property&status=SUBMITTED`

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

- Filter by `DRAFT`, `SUBMITTED`, or `CLOSED`.
- Combine status filtering with title search.
- Return all statuses when `status` is omitted.
- Return only cases belonging to the authenticated client.
- Exclude soft-deleted cases.
- Return an empty list when no cases match.
- Reject invalid status values with `400`.
- Follow the standard API error response format.

## Testing

- Client can filter cases by each status.
- Search and status filters work together.
- Omitting status returns cases of all statuses.
- No matches return an empty list.
- Invalid status returns `400`.
- Other clients' cases are excluded.
- Unauthenticated requests return `401`.

## Dependency

- CM-01-01: List My Cases API
- CM-04-01: Search Cases API
