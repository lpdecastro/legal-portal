# CM-01-01: List My Cases API

## Goal

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

- Return only cases belonging to the authenticated client.
- Exclude soft-deleted cases.
- Return an empty list when no cases exist.
- Never expose another client's cases.
- Follow the standard API error response format.

## Testing

- Client sees their own cases.
- Client cannot see another client's cases.
- Soft-deleted cases are excluded.
- Unauthenticated requests return `401`.
