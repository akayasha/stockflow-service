package com.stockflow.user.dto;

import com.stockflow.user.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull(message = "role is required")
        Role role
) {
}