package com.stockflow.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bean Validation unit tests for the auth DTOs.
 */
@DisplayName("Auth DTO validation unit tests")
class AuthDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    @DisplayName("Register: a valid email + 8+ char password has no violations")
    void registerValid() {
        Set<ConstraintViolation<RegisterRequest>> violations =
            validator.validate(new RegisterRequest("alice@example.com", "Demo1234!"));
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Register: malformed email is rejected")
    void registerInvalidEmail() {
        Set<ConstraintViolation<RegisterRequest>> violations =
            validator.validate(new RegisterRequest("not-an-email", "Demo1234!"));
        assertThat(violations)
            .anySatisfy(v -> assertThat(v.getPropertyPath()).hasToString("email"));
    }

    @Test
    @DisplayName("Register: short password is rejected (>= 8 chars required)")
    void registerShortPassword() {
        Set<ConstraintViolation<RegisterRequest>> violations =
            validator.validate(new RegisterRequest("alice@example.com", "short"));
        assertThat(violations)
            .anySatisfy(v -> {
                assertThat(v.getPropertyPath()).hasToString("password");
                assertThat(v.getMessage()).contains("between 8 and 100");
            });
    }

    @Test
    @DisplayName("Register: blank email is rejected")
    void registerBlankEmail() {
        Set<ConstraintViolation<RegisterRequest>> violations =
            validator.validate(new RegisterRequest(" ", "Demo1234!"));
        assertThat(violations)
            .anySatisfy(v -> assertThat(v.getPropertyPath()).hasToString("email"));
    }

    @Test
    @DisplayName("Login: valid email + non-blank password has no violations")
    void loginValid() {
        Set<ConstraintViolation<LoginRequest>> violations =
            validator.validate(new LoginRequest("alice@example.com", "whatever"));
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Login: blank password is rejected")
    void loginBlankPassword() {
        Set<ConstraintViolation<LoginRequest>> violations =
            validator.validate(new LoginRequest("alice@example.com", ""));
        assertThat(violations)
            .anySatisfy(v -> assertThat(v.getPropertyPath()).hasToString("password"));
    }
}
