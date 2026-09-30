package com.stockflow.common.security;

import java.util.UUID;

/**
 * Principal carried in the {@code SecurityContext} for authenticated requests.
 * Exposes the caller's UUID and email so services do not need to reach back
 * into the database just to know "who am I".
 */
public record AuthenticatedUser(
    UUID id,
    String email,
    String role
) {
}
