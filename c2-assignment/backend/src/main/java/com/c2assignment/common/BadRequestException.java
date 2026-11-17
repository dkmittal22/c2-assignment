package com.c2assignment.common;

/**
 * Raised for illegal requests that are not field-validation failures
 * (empty PATCH body, illegal query values). Maps to HTTP 400 BAD_REQUEST.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}