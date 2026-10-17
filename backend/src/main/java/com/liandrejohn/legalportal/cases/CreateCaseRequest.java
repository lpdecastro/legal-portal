package com.liandrejohn.legalportal.cases;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A new draft. Description and location can be filled in later, before the case is submitted. */
public record CreateCaseRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @NotNull(message = "Category is required")
        UUID categoryId,

        String description,

        @Size(max = 255, message = "Location must not exceed 255 characters")
        String location) {

    /** Normalizes input before validation: title and location are trimmed, and blank optional fields become null. */
    public CreateCaseRequest {
        title = title != null ? title.strip() : null;
        description = description != null && !description.isBlank() ? description : null;
        location = location != null && !location.isBlank() ? location.strip() : null;
    }

}
