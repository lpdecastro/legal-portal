# CASE-04-01: Get Case Details API

**Type:** BE  
**Story:** CASE-04 – View Case Details

## Description

Implement an API to retrieve an existing case belonging to the authenticated client.

The API will be used to populate the Edit Draft form and can be reused for the Case Details page.

## API Contract

**Endpoint:** `GET /api/v1/cases/{id}`  
**Authentication:** Required  
**CSRF:** Not required (read-only request)

### Path Parameters

- `id` – Required, UUID of the case.

### Success Response

`200 OK`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "title": "Employment dispute",
  "categoryId": "550e8400-e29b-41d4-a716-446655440000",
  "description": "I was terminated without prior notice.",
  "location": "Quezon City",
  "status": "DRAFT",
  "createdAt": "2026-10-15T04:00:00Z",
  "updatedAt": "2026-10-15T04:00:00Z"
}
```

### Error Responses

- `400 Bad Request` – Invalid case ID format.
- `401 Unauthorized` – Missing or invalid session.
- `404 Not Found` – Case does not exist, belongs to another client, or has been deleted.
- `500 Internal Server Error` – Unexpected server error.

Errors follow RFC 9457 Problem Details.

## Acceptance Criteria

1. Authenticated clients can retrieve their own cases.
2. API returns case details including title, category, description, location, status, and timestamps.
3. API supports `DRAFT`, `SUBMITTED`, and `CLOSED` cases.
4. Clients cannot retrieve another client's case.
5. Soft-deleted cases are not accessible.
6. Nonexistent or inaccessible cases return `404`.
7. Invalid case IDs return `400`.
8. Unauthenticated requests return `401`.
9. API errors follow the standard error response format.

## Testing

- Verify a client can retrieve their own draft case.
- Verify submitted and closed cases can be retrieved.
- Verify another client's case returns `404`.
- Verify soft-deleted cases return `404`.
- Verify nonexistent cases return `404`.
- Verify invalid UUID format returns `400`.
- Verify unauthenticated requests return `401`.
- Verify the response contains the expected case information.

## Dependencies

- CASE-02-01 – Create Draft API.
- Authentication and session management.

## Definition of Done

- All acceptance criteria are met.
- Clients can retrieve only their own non-deleted cases.
- Automated backend tests pass.
