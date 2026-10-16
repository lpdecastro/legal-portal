package com.liandrejohn.legalportal.auth;

import java.util.UUID;

/** The signed-in client, returned by login and {@code GET /api/v1/auth/me}. */
public record AuthenticatedClientResponse(UUID id, String email, String firstName, String lastName) {
}
