package com.liandrejohn.legalportal.auth;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.liandrejohn.legalportal.validation.MaxUtf8Bytes;

public record RegisterRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 12, message = "Password must contain at least 12 characters")
        @MaxUtf8Bytes(value = 72, message = "Password must not exceed 72 bytes")
        String password) {

    /** Normalizes input before validation: names and email are trimmed, and email is lowercased. */
    public RegisterRequest {
        firstName = firstName != null ? firstName.strip() : null;
        lastName = lastName != null ? lastName.strip() : null;
        email = email != null ? email.strip().toLowerCase(Locale.ROOT) : null;
    }

    @Override
    public String toString() {
        // Keep the password out of logs
        return "RegisterRequest[firstName=%s, lastName=%s, email=%s]".formatted(firstName, lastName, email);
    }

}
