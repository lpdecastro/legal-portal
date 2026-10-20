package com.liandrejohn.legalportal.error;

import java.util.List;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * A field value that passed Bean Validation but was rejected by a business rule, e.g. a category that doesn't exist.
 * Answered like a Bean Validation failure: 400 {@code VALIDATION_ERROR} with {@code fieldErrors}.
 */
@Getter
public class FieldValidationException extends ApiException {

    private final List<FieldErrorDetail> fieldErrors;

    public FieldValidationException(String field, String message) {
        this(List.of(new FieldErrorDetail(field, message)));
    }

    public FieldValidationException(List<FieldErrorDetail> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, ApiExceptionHandler.VALIDATION_ERROR, ApiExceptionHandler.VALIDATION_DETAIL);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

}
