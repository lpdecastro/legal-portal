# API Docs

Springdoc generates an OpenAPI 3.1 document from the controllers; Scalar renders it.

| Endpoint | Content | Toggle |
| --- | --- | --- |
| `GET /v3/api-docs` | OpenAPI JSON | `OPENAPI_ENABLED` (`springdoc.api-docs.enabled`) |
| `GET /scalar` | Scalar UI (reads `/v3/api-docs`) | `SCALAR_ENABLED` (`scalar.enabled`) |

- Both default to `true` and are public (`SecurityConfig`). Set them in `.env` or the environment and restart.
- Turning off OpenAPI also leaves Scalar with nothing to show, so turn off both.
- Turned off, the paths aren't served: signed-out requests get 401, like any unknown path.
- Title and version come from `app.openapi.title` / `app.openapi.version` (`OpenApiProperties`).
