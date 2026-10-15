# CM-06-01: Paginate Cases API

## Goal

Allow authenticated clients to retrieve their cases with server-side pagination.

## API Contract

**GET** `/api/v1/cases?page=0&size=10`

**Combined:** `GET /api/v1/cases?search=property&status=SUBMITTED&page=0&size=10`

**Query Parameters**

- `page` — Zero-based page index (default: `0`).
- `size` — Number of cases per page (default: `10`, max: `100`).
- `search` — Optional case title search.
- `status` — Optional case status filter.

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
  ],
  "page": {
    "size": 10,
    "number": 0,
    "totalElements": 25,
    "totalPages": 3
  }
}
```

**Errors**

- `400` — Invalid pagination parameters
- `401` — Unauthorized
- `500` — Internal server error

## Acceptance Criteria

- Return only the requested page of the authenticated client's cases.
- Use zero-based page indexing.
- Apply search and status filters before pagination.
- Sort by creation date descending by default, with a stable secondary sort by ID.
- Return pagination metadata.
- Enforce a maximum page size of `100`.
- Return an empty `content` array for pages beyond the last page.
- Reject negative page numbers and invalid page sizes.
- Exclude soft-deleted cases.
- Follow the standard API error response format.

## Testing

- Client can retrieve different pages of cases.
- Pagination metadata reflects the filtered results.
- Search and status filters work with pagination.
- Cases appear in a consistent order across pages.
- Out-of-range pages return an empty list.
- Invalid pagination parameters return `400`.
- Other clients' cases are excluded.
- Unauthenticated requests return `401`.

## Dependencies

- CM-01-01: List My Cases API
- CM-04-01: Search Cases API
- CM-05-01: Filter Cases API
