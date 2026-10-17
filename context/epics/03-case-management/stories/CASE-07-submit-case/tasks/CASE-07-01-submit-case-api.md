# CASE-07-01: Submit Case API

**Type:** BE  
**Story:** CASE-07 – Submit Case

## Description

Implement an API to submit an existing draft case belonging to the authenticated client.

## API Contract

**Endpoint:** `POST /api/v1/cases/{id}/submit`  
**Authentication:** Required  
**CSRF:** Required (`X-XSRF-TOKEN` header)

### Path Parameters

- `id` – Required, UUID of the case.

### Request Body

No request body required.

### Success Response

`200 OK`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "title": "Employment dispute",
  "categoryId": "550e8400-e29b-41d4-a716-446655440000",
  "description": "I was terminated without prior notice.",
  "location": "Quezon City",
  "status": "SUBMITTED",
  "createdAt": "2026-10-15T04:00:00Z",
  "updatedAt": "2026-10-15T05:00:00Z",
  "submittedAt": "2026-10-15T05:00:00Z"
}
```

### Error Responses

- `400 Bad Request` – Invalid case ID format or missing/invalid required case information.
- `401 Unauthorized` – Missing or invalid session.
- `403 Forbidden` – Missing or invalid CSRF token.
- `404 Not Found` – Case does not exist, belongs to another client, or has been deleted.
- `409 Conflict` – Case is not in `DRAFT` status.
- `500 Internal Server Error` – Unexpected server error.

Errors follow RFC 9457 Problem Details.

## Acceptance Criteria

1. Authenticated clients can submit their own draft cases.
2. Only cases with `DRAFT` status can be submitted.
3. Clients cannot submit another client's case.
4. Submission requires:
   - Title – Required, maximum 200 characters.
   - Legal Category – Required and active.
   - Description – Required, non-blank.
   - Location – Required, non-blank, maximum 255 characters.
5. Invalid or incomplete cases are rejected with validation errors.
6. Successful submission changes status from `DRAFT` to `SUBMITTED`.
7. Successful submission sets `submitted_at` and updates `updated_at`.
8. Original `created_at` remains unchanged.
9. Successful submission returns `200 OK` with the updated case.
10. Submitted and closed cases cannot be submitted again.
11. Soft-deleted cases cannot be submitted.
12. Failed submissions do not modify existing case data.
13. Concurrent submission requests cannot submit the same case more than once.
14. Missing or invalid CSRF tokens are rejected with `403`.
15. API errors follow the standard error response format.

## Testing

- Verify a valid draft can be submitted successfully.
- Verify status changes to `SUBMITTED`.
- Verify `submitted_at` and `updated_at` are populated.
- Verify `created_at` remains unchanged.
- Verify missing title, category, description, or location is rejected.
- Verify inactive or nonexistent categories are rejected.
- Verify submitted and closed cases return `409`.
- Verify another client's case returns `404`.
- Verify soft-deleted and nonexistent cases return `404`.
- Verify invalid UUID format returns `400`.
- Verify unauthenticated requests return `401`.
- Verify missing or invalid CSRF tokens return `403`.
- Verify concurrent submission requests cannot result in duplicate successful submissions.
- Verify failed requests leave the case unchanged.

## Dependencies

- CASE-02-01 – Create Draft API.
- CASE-04-01 – Get Case Details API.
- Authentication and CSRF protection.

## Definition of Done

- All acceptance criteria are met.
- Only valid draft cases belonging to the authenticated client can be submitted.
- Case status and submission timestamps are persisted correctly.
- Automated backend tests pass.
