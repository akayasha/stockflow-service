package com.stockflow.common.error;

import org.springframework.http.HttpStatus;

/**
 * Maps to HTTP 409 - used for business-rule conflicts that are not validation
 * errors: trying to delete a referenced product, illegal invoice status
 * transitions, stock guard violations, unique constraint violations.
 */
public class ConflictException extends ApiException {

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
