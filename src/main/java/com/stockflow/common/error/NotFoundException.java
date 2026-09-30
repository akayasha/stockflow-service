package com.stockflow.common.error;

import org.springframework.http.HttpStatus;

/**
 * Maps to HTTP 404. Used when a resource does not exist or is not visible to
 * the requesting user - the latter is intentional: returning 403 here would
 * leak the existence of someone else's record.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }

    public NotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
            resource + " with id " + id + " was not found");
    }
}
