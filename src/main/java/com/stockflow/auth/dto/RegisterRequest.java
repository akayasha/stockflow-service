package com.stockflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    @Size(max = 254, message = "email must be at most 254 characters")
    String email,

    @NotBlank(message = "password is required")
    @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
    String password
) {
}
