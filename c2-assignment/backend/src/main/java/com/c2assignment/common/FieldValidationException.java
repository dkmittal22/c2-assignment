package com.c2assignment.common;

import java.util.Map;

/**
 * Thrown when a semantically invalid field value (e.g. a whitespace-only string
 * on an optional PATCH field) is detected in the service layer. Mapped to
 * HTTP 400 with error code VALIDATION_ERROR and per-field messages.
 */
public class FieldValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public FieldValidationException(Map<String, String> fieldErrors) {
        super("Request validation failed");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}