package com.stockflow.common.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
/**
 * Centralised error rendering. Every uncaught exception in a controller flows
 * through here and gets shaped into the {@link ApiError} envelope.
 *
 * <p>Order of handlers matters: Spring picks the most specific match, so
 * {@link ApiException} must come before {@link Exception}, {@link BadCredentialsException}
 * before {@link AuthenticationException}, and so on.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {



    private static ResponseEntity<ApiErrorEnvelope> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ApiErrorEnvelope(ApiError.of(code, message, status.value())));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorEnvelope> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return respond(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Invalid value for parameter '" + ex.getName() + "'");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorEnvelope> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "Method " + ex.getMethod() + " is not supported for this endpoint");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorEnvelope> handleNoResource(NoResourceFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "No endpoint " + ex.getHttpMethod() + " /" + ex.getResourcePath());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorEnvelope> handleOptimisticLock(OptimisticLockingFailureException ex) {
        return respond(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The record was modified by another request. Please retry.");
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorEnvelope> handleApiException(ApiException ex, HttpServletRequest req) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("API exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        } else {
            log.debug("API exception on {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        }
        ApiError body = ApiError.of(ex.getCode(), ex.getMessage(), ex.getStatus().value());
        return ResponseEntity.status(ex.getStatus()).body(new ApiErrorEnvelope(body));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorEnvelope> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            // Use the last message for a field if multiple exist.
            fields.put(fe.getField(), fe.getDefaultMessage());
        }
        ApiError body = ApiError.of(
            "VALIDATION_FAILED",
            "Request validation failed",
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            fields
        );
        return ResponseEntity.unprocessableEntity().body(new ApiErrorEnvelope(body));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorEnvelope> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Map unique constraint violations to a 409 with a stable code. We do
        // not leak the underlying SQL exception message because it can carry
        // column names a caller should not rely on.
        log.debug("Data integrity violation", ex);
        ApiError body = ApiError.of(
            "CONSTRAINT_VIOLATION",
            "The request violates a data integrity constraint",
            HttpStatus.CONFLICT.value()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorEnvelope(body));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorEnvelope> handleBadCredentials(BadCredentialsException ex) {
        // Generic message - never reveal whether the email exists.
        ApiError body = ApiError.of(
            "UNAUTHORIZED",
            UnauthorizedException.GENERIC_MESSAGE,
            HttpStatus.UNAUTHORIZED.value()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiErrorEnvelope(body));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorEnvelope> handleAuthentication(AuthenticationException ex) {
        ApiError body = ApiError.of(
            "UNAUTHORIZED",
            "Authentication is required to access this resource",
            HttpStatus.UNAUTHORIZED.value()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiErrorEnvelope(body));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorEnvelope> handleAccessDenied(AccessDeniedException ex) {
        ApiError body = ApiError.of(
            "FORBIDDEN",
            "You are not allowed to perform this action",
            HttpStatus.FORBIDDEN.value()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiErrorEnvelope(body));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorEnvelope> handleNotFound(NoHandlerFoundException ex) {
        ApiError body = ApiError.of(
            "RESOURCE_NOT_FOUND",
            "No endpoint " + ex.getHttpMethod() + " " + ex.getRequestURL(),
            HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorEnvelope(body));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorEnvelope> handleUnknown(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        ApiError body = ApiError.of(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorEnvelope(body));
    }
}
