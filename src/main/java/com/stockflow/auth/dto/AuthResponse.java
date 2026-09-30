package com.stockflow.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(
    UUID userId,
    String email,
    String role,
    String token,
    Instant expiresAt
) {
}
