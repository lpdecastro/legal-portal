# DASH-01-01: Dashboard Summary API

## Task

Create an API to retrieve case statistics and recent cases for the logged-in client.

## Acceptance Criteria

- Return total, draft, submitted, and closed case counts.
- Return the 5 most recent cases, ordered by creation date (newest first).
- Include only the authenticated client's non-deleted cases.
- Return zero counts and an empty list when no cases exist.
- Reject unauthenticated requests.

## API Contract

**GET** `/api/v1/dashboard/summary`

**Response: 200 OK**

```json
{
  "statistics": {
    "total": 12,
    "draft": 3,
    "submitted": 7,
    "closed": 2
  },
  "recentCases": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Property Dispute",
      "status": "SUBMITTED",
      "createdAt": "2026-10-15T08:00:00Z"
    }
  ]
}
```

**Errors**

- `401 UNAUTHORIZED` — Missing or expired session.
