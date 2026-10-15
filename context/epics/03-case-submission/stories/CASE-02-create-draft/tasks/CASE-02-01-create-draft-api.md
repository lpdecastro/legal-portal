# CASE-02-01: Create Draft API

**Type:** BE  
**Story:** CASE-02 – Create Draft Case

## Description

Implement an API to create a draft case associated with the authenticated client.

## API Contract

**Endpoint:** `POST /api/v1/cases`  
**Authentication:** Required

### Request Body

```json
{
  "title": "Employment dispute",
  "categoryId": "550e8400-e29b-41d4-a716-446655440000",
  "description": "I was terminated without prior notice.",
  "location": "Quezon City"
}
```

- `title` – Required, maximum 200 characters.
- `categoryId` – Required, must reference an active legal category.
- `description` – Optional.
- `location` – Optional, maximum 255 characters.

### Success Response

`201 Created`

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

Response header:

`Location: /api/v1/cases/{id}`

### Error Responses

- `400 Bad Request` – Missing title/category or invalid field values.
- `401 Unauthorized` – Missing or invalid session.
- `400 Bad Request` – Invalid or inactive legal category.
- `500 Internal Server Error` – Unexpected server error.

Errors follow RFC 9457 Problem Details.

## Acceptance Criteria

1. Authenticated clients can create a draft case.
2. Title and active legal category are required.
3. Description and location can be omitted.
4. Invalid or inactive legal categories are rejected.
5. New cases automatically receive `DRAFT` status.
6. Case ownership is assigned from the authenticated session, not the request body.
7. Successful creation returns `201 Created` with the created case.
8. Invalid inputs return appropriate validation errors.
9. Unauthenticated requests return `401`.
10. API errors follow the standard error response format.

## Testing

- Verify a draft is created successfully.
- Verify omitted optional fields are accepted.
- Verify required field validation.
- Verify inactive or nonexistent categories are rejected.
- Verify the case belongs to the authenticated client.
- Verify the status is `DRAFT`.
- Verify unauthenticated access is rejected.
- Verify invalid requests do not create cases.
