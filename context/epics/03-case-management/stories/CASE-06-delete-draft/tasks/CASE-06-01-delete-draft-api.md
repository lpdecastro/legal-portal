# CASE-06-01: Delete Draft API

**Type:** BE  
**Story:** CASE-06 – Delete Draft Case

## Description

Implement an API to soft-delete an existing draft case belonging to the authenticated client.

## API Contract

**Endpoint:** `DELETE /api/v1/cases/{id}`  
**Authentication:** Required  
**CSRF:** Required (`X-XSRF-TOKEN` header)

### Path Parameters

- `id` – Required, UUID of the case.

### Success Response

`204 No Content`

No response body.

### Error Responses

- `400 Bad Request` – Invalid case ID format.
- `401 Unauthorized` – Missing or invalid session.
- `403 Forbidden` – Missing or invalid CSRF token.
- `404 Not Found` – Case does not exist, belongs to another client, or has already been deleted.
- `409 Conflict` – Case is not in `DRAFT` status.
- `500 Internal Server Error` – Unexpected server error.

Errors follow RFC 9457 Problem Details.

## Acceptance Criteria

1. Authenticated clients can delete their own draft cases.
2. Only cases with `DRAFT` status can be deleted.
3. Clients cannot delete another client's case.
4. Deletion is performed by setting `deleted_at` to the current timestamp.
5. Soft-deleted cases are excluded from subsequent case retrievals.
6. Successful deletion returns `204 No Content`.
7. Repeated deletion of the same case returns `404`.
8. Submitted and closed cases cannot be deleted and return `409`.
9. Invalid requests do not modify the case.
10. Missing or invalid CSRF tokens are rejected with `403`.
11. API errors follow the standard error response format.

## Testing

- Verify an existing draft can be deleted successfully.
- Verify `deleted_at` is populated after deletion.
- Verify the record remains in the database.
- Verify deleted cases cannot be retrieved through the API.
- Verify repeated deletion returns `404`.
- Verify submitted and closed cases return `409`.
- Verify another client's case returns `404`.
- Verify nonexistent cases return `404`.
- Verify invalid UUID format returns `400`.
- Verify unauthenticated requests return `401`.
- Verify missing or invalid CSRF tokens return `403`.
- Verify failed requests do not modify existing data.

## Dependencies

- CASE-02-01 – Create Draft API.
- CASE-04-01 – Get Case Details API.
- Authentication and CSRF protection.

## Definition of Done

- All acceptance criteria are met.
- Only the authenticated client's draft cases can be soft-deleted.
- Automated backend tests pass.
