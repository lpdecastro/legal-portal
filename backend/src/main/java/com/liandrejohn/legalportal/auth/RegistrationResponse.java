package com.liandrejohn.legalportal.auth;

import java.util.UUID;

public record RegistrationResponse(UUID id, String email, String firstName, String lastName) {
}
