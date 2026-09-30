package com.stockflow.common.error;

import org.springframework.http.HttpStatus;

/**
 * Maps to HTTP 401. Thrown by the auth filter or auth service when credentials
 * are missing or invalid. Always carries the same generic message so callers
 * cannot distinguish "user not found" from "wrong password".
 */
public class UnauthorizedException extends ApiException {

    public static final String GENERIC_MESSAGE = "Invalid email or password";

    public UnauthorizedException() {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", GENERIC_MESSAGE);
    }

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }
}
