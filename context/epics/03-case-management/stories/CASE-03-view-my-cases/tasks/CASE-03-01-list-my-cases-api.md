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

## Testing

- Client sees their own cases.
- Client cannot see another client's cases.
- Soft-deleted cases are excluded.
- Unauthenticated requests return `401`.

## Dependencies

- Authentication and session management.

## Definition of Done

- All acceptance criteria are met.
- Automated BE tests pass.
