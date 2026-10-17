# CASE-05-01: Update Draft API

**Type:** BE  
**Story:** CASE-05 – Edit Draft Case

## Description

Implement an API to update an existing draft case belonging to the authenticated client.

## API Contract

**Endpoint:** `PATCH /api/v1/cases/{id}`  
**Authentication:** Required  
**CSRF:** Required (`X-XSRF-TOKEN` header)

### Request Body

```json
{
  "title": "Updated employment dispute",
  "categoryId": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Updated details about my employment dispute.",
  "location": "Quezon City"
}
```

- `title` – Required, maximum 200 characters.
- `categoryId` – Required, must reference an active legal category.
- `description` – Optional, nullable.
- `location` – Optional, nullable, maximum 255 characters.
- Only provided fields are updated. Omitted fields remain unchanged.
- Explicit `null` clears optional fields.

### Success Response

`200 OK`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "title": "Updated employment dispute",
  "categoryId": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Updated details about my employment dispute.",
  "location": "Quezon City",
  "status": "DRAFT",
  "createdAt": "2026-10-15T04:00:00Z",
  "updatedAt": "2026-10-15T05:00:00Z"
}
```

### Error Responses

- `400 Bad Request` – Invalid field values or inactive/nonexistent category.
- `401 Unauthorized` – Missing or invalid session.
- `403 Forbidden` – Missing or invalid CSRF token.
- `404 Not Found` – Case does not exist or belongs to another client.
- `409 Conflict` – Case is not in `DRAFT` status.
- `500 Internal Server Error` – Unexpected server error.

Errors follow RFC 9457 Problem Details.

## Acceptance Criteria

1. Authenticated clients can update their own draft cases.
2. Only cases with `DRAFT` status can be updated.
3. Clients cannot update another client's case.
4. Only provided fields are updated.
5. Title and category cannot be cleared.
6. Category must reference an active legal category.
7. Description and location can be cleared.
8. Successful updates return `200 OK` with the updated case.
9. Updated cases retain `DRAFT` status and their original creation timestamp.
10. Invalid requests do not modify the case.
11. Missing or invalid CSRF tokens are rejected with `403`.
12. API errors follow the standard error response format.

## Testing

- Verify a draft can be updated successfully.
- Verify partial updates preserve omitted fields.
- Verify optional fields can be cleared.
- Verify required field and length validation.
- Verify inactive or nonexistent categories are rejected.
- Verify submitted and closed cases cannot be updated.
- Verify another client's case cannot be updated.
- Verify nonexistent cases return `404`.
- Verify unauthenticated requests return `401`.
- Verify missing or invalid CSRF tokens return `403`.
- Verify invalid requests do not modify existing data.

## Dependencies

- CASE-02-01 – Create Draft API.
- CASE-04-01 – Get Case Details API.
- Authentication and CSRF protection.
