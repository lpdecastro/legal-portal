# CM-04-01: Search Cases API

## Goal

Allow authenticated clients to search their own cases by title.

## API Contract

**GET** `/api/v1/cases?search=property`

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

- Search case titles using partial, case-insensitive matching.
- Return only cases belonging to the authenticated client.
- Exclude soft-deleted cases.
- Return an empty list when no cases match.
- Return all client's cases when `search` is empty or omitted.
- Follow the standard API error response format.

## Testing

- Client can search cases by partial title.
- Search is case-insensitive.
- Non-matching queries return an empty list.
- Other clients' cases are excluded.
- Unauthenticated requests return `401`.

## Dependency

- CM-01-01: List My Cases API
