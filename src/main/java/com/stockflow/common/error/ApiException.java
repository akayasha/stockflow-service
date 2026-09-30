package com.stockflow.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for domain errors that map cleanly onto HTTP status codes.
 *
 * <p>Throwing one of these from a service or controller triggers
 * {@link com.stockflow.common.error.GlobalExceptionHandler} to render the
 * standard {@code ApiError} JSON envelope.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public ApiException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }
}
