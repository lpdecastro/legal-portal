# CASE-11-01: Search Cases API

**Type:** BE  
**Story:** CASE-11 – Search Cases

## Description

Allow authenticated clients to search their own cases by title.

## API Contract

**GET** `/api/v1/cases?search=property`

**Combined:** `GET /api/v1/cases?search=property&status=SUBMITTED&page=0&size=10`

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

- `401` — Unauthorized
- `500` — Internal server error

## Acceptance Criteria

1. Search case titles using partial, case-insensitive matching.
2. Return only cases belonging to the authenticated client.
3. Exclude soft-deleted cases.
4. Return an empty list when no cases match.
5. Return all client's cases when `search` is empty or omitted.
6. Combine title search with the status filter; apply both before pagination.
7. Follow the standard API error response format.

## Testing

- Client can search cases by partial title.
- Search is case-insensitive.
- Non-matching queries return an empty list.
- Search and status filters work together with pagination.
- Other clients' cases are excluded.
- Unauthenticated requests return `401`.

## Dependencies

- CASE-03-01 – List My Cases API.
- CASE-10-01 – Filter Cases API.
- Authentication and session management.

## Definition of Done

- All acceptance criteria are met.
- Automated BE tests pass.
