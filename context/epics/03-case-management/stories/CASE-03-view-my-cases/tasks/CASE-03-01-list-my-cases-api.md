# CASE-03-01: List My Cases API

**Type:** BE  
**Story:** CASE-03 – View My Cases

## Description

Allow authenticated clients to retrieve their own cases.

## API Contract

**GET** `/api/v1/cases`

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

1. Return only cases belonging to the authenticated client.
2. Exclude soft-deleted cases.
3. Return an empty list when no cases exist.
4. Never expose another client's cases.
5. Follow the standard API error response format.
6. Include cases in every status (`DRAFT`, `SUBMITTED`, `CLOSED`).
7. `category` is the category name.
8. Sort newest first (`createdAt` descending).
9. Reject suspended, disabled or deleted accounts with `401`.

## Testing

- Client sees their own cases.
- Client cannot see another client's cases.
- Soft-deleted cases are excluded.
- Cases in every status are listed, newest first.
- Client with no cases gets an empty `content` list.
- Unauthenticated requests return `401`.
- Suspended clients get `401`.

## Dependencies

- Authentication and session management.

## Definition of Done

- All acceptance criteria are met.
- Automated BE tests pass.
