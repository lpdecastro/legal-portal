# Partial Case Updates

How `PATCH /api/v1/cases/{id}` treats fields that are left out of the body versus fields that are cleared. Written during CASE-05-01.

## Left out vs cleared

[`UpdateCaseRequest`](../backend/src/main/java/com/liandrejohn/legalportal/cases/UpdateCaseRequest.java) holds each field as an `Optional`:

- **Left out** of the JSON body: the field stays `null` in the request DTO, and the stored value is kept.
- **Sent as `null`**: Jackson calls the setter with an empty `Optional`, and the field is cleared.

The request is a class with setters, not a record. Jackson passes an empty `Optional` to a record's constructor for left-out fields too, so a record can't tell the two apart. It only calls a setter for fields that are present in the body.

## `description` and `location`

| Frontend sends | Request DTO value | Result |
| --- | --- | --- |
| Field left out | `null` | Keeps the current value |
| `null` | Empty `Optional` | Cleared (stored as `null`) |
| `""` or `"   "` | Empty `Optional` | Cleared (stored as `null`) |
| `" Makati "` | `Optional["Makati"]` | Set (location is trimmed; description is not) |

## `title` and `categoryId`

| Frontend sends | Request DTO value | Result |
| --- | --- | --- |
| Field left out | `null` | Keeps the current value |
| `null` | Empty `Optional` | 400 `VALIDATION_ERROR` "… is required" |
| `""` or `"   "` (title) | `Optional[""]` | 400 `VALIDATION_ERROR` "Title is required" |
| A valid value | `Optional[value]` | Updated (title is trimmed) |

Length limits and the active-category check apply only to fields that are sent.

## Edit Draft form

Per CASE-05-02, Save Changes sends only the modified fields:

- Unchanged fields are left out.
- A field the user emptied is sent as `null` (or `""`) to clear it.
