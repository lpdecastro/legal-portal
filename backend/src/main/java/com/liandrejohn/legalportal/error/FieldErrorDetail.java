package com.liandrejohn.legalportal.error;

/** One entry of the {@code fieldErrors} Problem Details extension. */
public record FieldErrorDetail(String field, String message) {
}
