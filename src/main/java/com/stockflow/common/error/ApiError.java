package com.stockflow.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Standard JSON error envelope returned by every error path.
 *
 * <p>Shape (intentionally flat under a single {@code error} key so clients can
 * rely on one place to look):
 * <pre>
 * {
 *   "error": {
 *     "code": "STOCK_INSUFFICIENT",
 *     "message": "Not enough stock for product 'Buku Tulis'",
 *     "status": 409,
 *     "fields": { "items[0].quantity": "must be greater than available stock" },
 *     "traceId": "...",
 *     "timestamp": "2026-09-29T10:00:00Z"
 *   }
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    String code,
    String message,
    int status,
    Map<String, String> fields,
    String traceId,
    Instant timestamp
) {
    public static ApiError of(String code, String message, int status) {
        return new ApiError(code, message, status, null, null, Instant.now());
    }

    public static ApiError of(String code, String message, int status, Map<String, String> fields) {
        return new ApiError(code, message, status, fields, null, Instant.now());
    }
}
