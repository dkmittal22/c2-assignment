package com.c2assignment.common;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception handling producing the consistent error structure
 * defined in spec/api-contract.md §3.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String INVALID_TRANSITION = "INVALID_TRANSITION";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                 HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return validation(request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> {
            String field = v.getPropertyPath() == null ? "" : v.getPropertyPath().toString();
            fieldErrors.putIfAbsent(field, v.getMessage());
        });
        return validation(request, fieldErrors);
    }

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<ApiError> handleFieldValidation(FieldValidationException ex,
                                                          HttpServletRequest request) {
        return validation(request, ex.getFieldErrors());
    }

    /**
     * Handles unreadable request bodies: unknown JSON fields and malformed JSON
     * become BAD_REQUEST, invalid enum values become VALIDATION_ERROR.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex,
                                                      HttpServletRequest request) {
        Throwable cause = ex.getCause();

        if (cause instanceof UnrecognizedPropertyException unknown) {
            return badRequest("Unknown field '" + unknown.getPropertyName() + "' is not allowed", request);
        }

        // Unwrap nested mapping exceptions so enum failures are recognised as validation.
        Throwable nested = cause;
        while (nested != null) {
            if (nested instanceof UnrecognizedPropertyException unknown) {
                return badRequest("Unknown field '" + unknown.getPropertyName() + "' is not allowed", request);
            }
            if (nested instanceof InvalidFormatException invalidFormat) {
                if (isEnum(invalidFormat)) {
                    return validation(request, enumFieldError(invalidFormat));
                }
                return badRequest(describe(invalidFormat), request);
            }
            nested = nested.getCause();
        }

        if (cause instanceof JsonMappingException mappingException) {
            return badRequest(describe(mappingException), request);
        }

        return badRequest("Malformed request body", request);
    }

    /**
     * Malformed path UUIDs and non-integer page/size become BAD_REQUEST;
     * invalid enum query values (e.g. ?status=BOGUS) become VALIDATION_ERROR.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        Class<?> requiredType = ex.getRequiredType();
        if (requiredType != null && requiredType.isEnum()) {
            Map<String, String> fieldErrors = new LinkedHashMap<>();
            fieldErrors.put(ex.getName(), "Invalid value '" + ex.getValue() + "'. Must be one of: "
                    + Arrays.stream(requiredType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", ")));
            return validation(request, fieldErrors);
        }
        return badRequest("Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'", request);
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(TicketNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<ApiError> handleInvalidTransition(InvalidTransitionException ex,
                                                            HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, INVALID_TRANSITION, ex.getMessage(), request, null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return badRequest(ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception while processing {} {}", request.getMethod(), request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR, "Unexpected server error", request, null);
    }

    private boolean isEnum(InvalidFormatException ex) {
        return ex.getTargetType() != null && ex.getTargetType().isEnum();
    }

    private Map<String, String> enumFieldError(InvalidFormatException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        String field = ex.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse("value");
        fieldErrors.put(field, "Invalid value '" + ex.getValue() + "'. Must be one of: "
                + Arrays.stream(ex.getTargetType().getEnumConstants())
                .map(String::valueOf)
                .collect(Collectors.joining(", ")));
        return fieldErrors;
    }

    private String describe(JsonMappingException ex) {
        return "Malformed request body: " + ex.getOriginalMessage();
    }

    private ResponseEntity<ApiError> validation(HttpServletRequest request, Map<String, String> fieldErrors) {
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "Request validation failed", request, fieldErrors);
    }

    private ResponseEntity<ApiError> badRequest(String message, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, BAD_REQUEST, message, request, null);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                           HttpServletRequest request, Map<String, String> fieldErrors) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}