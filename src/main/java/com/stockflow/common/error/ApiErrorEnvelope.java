package com.stockflow.common.error;

/**
 * Wrapper so the JSON response is {@code {"error": { ... }}} rather than the
 * bare {@link ApiError} record. This keeps room to grow (e.g. add
 * {@code warnings} or {@code meta}) without breaking the public contract.
 */
public record ApiErrorEnvelope(ApiError error) {
}
