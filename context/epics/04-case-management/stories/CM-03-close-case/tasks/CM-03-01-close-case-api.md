# CM-03-01: Close Case API

## Goal

Allow authenticated clients to close their own submitted cases.

## API Contract

**POST** `/api/v1/cases/{id}/close`

**Response: 200 OK**

```json
{
  "id": "uuid",
  "status": "CLOSED",
  "closedAt": "2026-10-15T08:00:00Z"
}
```

**Errors**

- `401` — Unauthorized
- `404` — Case not found or not owned by client
- `409` — Invalid case status
- `500` — Internal server error

## Acceptance Criteria

- Only the case owner can close the case.
- Only `SUBMITTED` cases can be closed.
- Update status to `CLOSED` and set `closedAt`.
- Reject closing `DRAFT` or `CLOSED` cases.
- Follow the standard API error response format.

## Testing

- Client can close their own submitted case.
- Closed case returns updated status and closure date.
- Closing a draft or closed case returns `409`.
- Closing another client's case returns `404`.
- Unauthenticated requests return `401`.
