package com.liandrejohn.legalportal.cases;

import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * Changes to a draft. A field left out of the body stays {@code null} and is unchanged; a field sent as JSON
 * {@code null} is an empty {@link Optional} and clears the value, which title and category don't allow. Constraints
 * apply to the value inside the {@code Optional} and are skipped for omitted fields.
 *
 * <p>
 * A class with setters rather than a record: Jackson passes an empty {@code Optional} to a record's constructor for
 * omitted fields too, but only calls a setter for fields present in the body.
 */
@Getter
public class UpdateCaseRequest {

    private Optional<@NotBlank(message = "Title is required")
                     @Size(max = 200, message = "Title must not exceed 200 characters") String> title;

    private Optional<@NotNull(message = "Category is required") UUID> categoryId;

    private Optional<String> description;

    private Optional<@Size(max = 255, message = "Location must not exceed 255 characters") String> location;

    // Setters normalize input like CreateCaseRequest: title and location are trimmed, blank optional fields become empty

    public void setTitle(Optional<String> title) {
        this.title = title.map(String::strip);
    }

    public void setCategoryId(Optional<UUID> categoryId) {
        this.categoryId = categoryId;
    }

    public void setDescription(Optional<String> description) {
        this.description = description.filter((value) -> !value.isBlank());
    }

    public void setLocation(Optional<String> location) {
        this.location = location.map(String::strip).filter((value) -> !value.isEmpty());
    }

}
