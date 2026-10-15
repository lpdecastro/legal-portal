# CASE-01-01: List Legal Categories API

**Type:** BE  
**Story:** CASE-01 – Legal Categories

## Description

Implement an API to retrieve active legal categories for the Create/Edit Draft dropdown.

## API Contract

**Endpoint:** `GET /api/v1/legal-categories`  
**Authentication:** Required

### Success Response

`200 OK`

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Family Law",
    "description": "Divorce, custody, and other family matters"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "name": "Employment Law",
    "description": "Workplace disputes and employment issues"
  }
]
```

If no active categories exist, return `200 OK` with `[]`.

### Error Responses

- `401 Unauthorized` – Missing or invalid session.
- `500 Internal Server Error` – Unexpected server error.

Errors follow RFC 9457 Problem Details.

## Acceptance Criteria

1. Authenticated clients can retrieve legal categories.
2. Only categories with `is_active = true` are returned.
3. Categories are sorted alphabetically by name.
4. Each category includes `id`, `name`, and `description`.
5. No active categories returns an empty array.
6. Unauthenticated requests return `401`.
7. API errors follow the standard error response format.

## Testing

- Verify active categories are returned.
- Verify inactive categories are excluded.
- Verify alphabetical sorting.
- Verify empty results.
- Verify unauthenticated access is rejected.
