package com.liandrejohn.legalportal.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.liandrejohn.legalportal.validation.MaxUtf8Bytes;

/** Passwords are compared as sent, never trimmed, matching registration and login. */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 12, message = "Password must contain at least 12 characters")
        @MaxUtf8Bytes(value = 72, message = "Password must not exceed 72 bytes")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        String confirmPassword) {

    @Override
    public String toString() {
        // Keep the passwords out of logs
        return "ChangePasswordRequest[]";
    }

}
