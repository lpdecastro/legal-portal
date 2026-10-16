package com.liandrejohn.legalportal.auth;

import java.util.UUID;

public record LoginResponse(UUID id, String email, String firstName, String lastName) {
}
