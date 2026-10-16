package com.liandrejohn.legalportal.auth;

import java.util.Locale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @NotBlank(message = "Password is required")
        String password) {

    /** Normalizes the email the same way registration does: trimmed and lowercased. */
    public LoginRequest {
        email = email != null ? email.strip().toLowerCase(Locale.ROOT) : null;
    }

    @Override
    public String toString() {
        // Keep the password out of logs
        return "LoginRequest[email=%s]".formatted(email);
    }

}
