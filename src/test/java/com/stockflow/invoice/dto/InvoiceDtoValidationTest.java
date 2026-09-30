package com.stockflow.invoice.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bean Validation unit tests for the invoice DTOs.
 */
@DisplayName("Invoice DTO validation unit tests")
class InvoiceDtoValidationTest {

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
    @DisplayName("InvoiceRequest: a complete valid request has no violations")
    void invoiceRequestValid() {
        InvoiceRequest req = new InvoiceRequest(
            "Customer A",
            null,
            null,
            "Net 30",
            List.of(new InvoiceItemRequest(UUID.randomUUID(), 2)));
        Set<ConstraintViolation<InvoiceRequest>> violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("InvoiceRequest: empty items list is rejected")
    void invoiceRequestEmptyItems() {
        InvoiceRequest req = new InvoiceRequest(
            "Customer A", null, null, null, List.of());
        Set<ConstraintViolation<InvoiceRequest>> violations = validator.validate(req);
        assertThat(violations)
            .anySatisfy(v -> assertThat(v.getPropertyPath()).hasToString("items"));
    }

    @Test
    @DisplayName("InvoiceRequest: blank customerName is rejected")
    void invoiceRequestBlankCustomer() {
        InvoiceRequest req = new InvoiceRequest(
            " ", null, null, null,
            List.of(new InvoiceItemRequest(UUID.randomUUID(), 1)));
        Set<ConstraintViolation<InvoiceRequest>> violations = validator.validate(req);
        assertThat(violations)
            .anySatisfy(v -> assertThat(v.getPropertyPath()).hasToString("customerName"));
    }

    @Test
    @DisplayName("InvoiceItemRequest: zero quantity is rejected (> 0 required)")
    void itemZeroQuantity() {
        InvoiceItemRequest item = new InvoiceItemRequest(UUID.randomUUID(), 0);
        Set<ConstraintViolation<InvoiceItemRequest>> violations = validator.validate(item);
        assertThat(violations)
            .anySatisfy(v -> {
                assertThat(v.getPropertyPath()).hasToString("quantity");
                assertThat(v.getMessage()).contains("greater than zero");
            });
    }

    @Test
    @DisplayName("InvoiceItemRequest: missing productId is rejected")
    void itemMissingProductId() {
        InvoiceItemRequest item = new InvoiceItemRequest(null, 1);
        Set<ConstraintViolation<InvoiceItemRequest>> violations = validator.validate(item);
        assertThat(violations)
            .anySatisfy(v -> assertThat(v.getPropertyPath()).hasToString("productId"));
    }
}
