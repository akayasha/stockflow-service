package com.stockflow.product.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bean Validation unit tests for {@link ProductRequest}. Runs Hibernate
 * Validator standalone - no Spring context, no database.
 */
@DisplayName("ProductRequest validation unit tests")
class ProductRequestValidationTest {

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
    @DisplayName("A valid request has zero violations")
    void validRequest() {
        ProductRequest req = new ProductRequest(
            "SKU-001", "Notebook", "Hardcover",
            new BigDecimal("35000.00"), 10);

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Blank SKU is rejected with a clear field message")
    void blankSkuRejected() {
        ProductRequest req = new ProductRequest(
            " ", "Notebook", null,
            new BigDecimal("100.00"), 1);

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        assertThat(violations)
            .anySatisfy(v -> {
                assertThat(v.getPropertyPath()).hasToString("sku");
                assertThat(v.getMessage()).isEqualTo("sku is required");
            });
    }

    @Test
    @DisplayName("Blank name is rejected")
    void blankNameRejected() {
        ProductRequest req = new ProductRequest(
            "SKU-001", "", null,
            new BigDecimal("100.00"), 1);

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        assertThat(violations)
            .anySatisfy(v -> {
                assertThat(v.getPropertyPath()).hasToString("name");
                assertThat(v.getMessage()).isEqualTo("name is required");
            });
    }

    @Test
    @DisplayName("Null unitPrice is rejected (not just zero)")
    void nullUnitPriceRejected() {
        ProductRequest req = new ProductRequest(
            "SKU-001", "Notebook", null,
            null, 1);

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        assertThat(violations)
            .anySatisfy(v -> {
                assertThat(v.getPropertyPath()).hasToString("unitPrice");
                assertThat(v.getMessage()).isEqualTo("unitPrice is required");
            });
    }

    @Test
    @DisplayName("Negative unitPrice is rejected")
    void negativeUnitPriceRejected() {
        ProductRequest req = new ProductRequest(
            "SKU-001", "Notebook", null,
            new BigDecimal("-1.00"), 1);

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        assertThat(violations)
            .anySatisfy(v -> assertThat(v.getPropertyPath()).hasToString("unitPrice"));
    }

    @Test
    @DisplayName("Zero unitPrice is allowed (free items are legal)")
    void zeroUnitPriceAllowed() {
        ProductRequest req = new ProductRequest(
            "SKU-001", "Sample", null,
            BigDecimal.ZERO, 1);

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Negative quantity is rejected")
    void negativeQuantityRejected() {
        ProductRequest req = new ProductRequest(
            "SKU-001", "Notebook", null,
            new BigDecimal("100.00"), -3);

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        assertThat(violations)
            .anySatisfy(v -> assertThat(v.getPropertyPath()).hasToString("quantityOnHand"));
    }

    @Test
    @DisplayName("Zero quantity is allowed (out-of-stock products still exist as records)")
    void zeroQuantityAllowed() {
        ProductRequest req = new ProductRequest(
            "SKU-001", "Backorder", null,
            new BigDecimal("100.00"), 0);

        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }
}
