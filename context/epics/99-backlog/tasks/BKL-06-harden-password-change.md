# BKL-06: Harden Password Change

**Type:** BE  
**Epic:** BKL – Backlog

## Objective

Limit current-password guessing on `PATCH /api/v1/clients/me/password` and sign out other sessions after a change.

## Acceptance Criteria

1. Repeated `INCORRECT_PASSWORD` failures for one account return 429 `RATE_LIMIT_EXCEEDED`, like login.
2. A successful change ends the client's other sessions; the current session stays signed in.
3. Integration tests cover both.

## Dependencies

- PROF-02-01, AUTH-02-01.
