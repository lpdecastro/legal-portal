package com.liandrejohn.legalportal.client;

import java.util.Optional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * Changes to the client's profile, with the same partial-update semantics as {@code UpdateCaseRequest}: a field left
 * out of the body stays {@code null} and is unchanged; a field sent as JSON {@code null} is an empty {@link Optional}
 * and clears the value, which first and last name don't allow. Email isn't a field, so it can't be changed.
 */
@Getter
public class UpdateProfileRequest {

    private Optional<@NotBlank(message = "First name is required")
                     @Size(max = 100, message = "First name must not exceed 100 characters") String> firstName;

    private Optional<@NotBlank(message = "Last name is required")
                     @Size(max = 100, message = "Last name must not exceed 100 characters") String> lastName;

    private Optional<@Size(max = 30, message = "Phone number must not exceed 30 characters")
                     @Pattern(regexp = "\\+?[0-9][0-9 ()-]*[0-9]",
                             message = "Phone number may only contain digits, spaces, parentheses, hyphens and a leading +")
                     String> phone;

    // Setters normalize input like RegisterRequest: values are trimmed, and a blank phone becomes empty (cleared)

    public void setFirstName(Optional<String> firstName) {
        this.firstName = firstName.map(String::strip);
    }

    public void setLastName(Optional<String> lastName) {
        this.lastName = lastName.map(String::strip);
    }

    public void setPhone(Optional<String> phone) {
        this.phone = phone.map(String::strip).filter((value) -> !value.isEmpty());
    }

}
