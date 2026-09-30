package com.stockflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockflow.auth.dto.LoginRequest;
import com.stockflow.auth.dto.RegisterRequest;
import com.stockflow.product.dto.ProductRequest;
import com.stockflow.testsupport.PostgresTestContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = PostgresTestContainer.Initializer.class)
@DisplayName("StockFlow end-to-end integration tests")
@EnabledIfEnvironmentVariable(named = PostgresTestContainer.INTEGRATION_FLAG, matches = "true")
@Testcontainers(disabledWithoutDocker = true)
class StockFlowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    private MockMvc mockMvc;

    private MockMvc mvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        }
        return mockMvc;
    }

    private String registerAndLogin(String email) throws Exception {
        mvc().perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, "Demo1234!"))))
            .andExpect(status().isCreated());

        MvcResult login = mvc().perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, "Demo1234!"))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
    }

    private UUID createProduct(String token, String sku, int qty, String price) throws Exception {
        ProductRequest req = new ProductRequest(sku, "Product " + sku, "Desc",
            new BigDecimal(price), qty);
        MvcResult r = mvc().perform(post("/api/products")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn();
        return UUID.fromString(objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }

    @Test
    @DisplayName("Login with the wrong password is rejected with a generic 401 message")
    void wrongPasswordIsRejected() throws Exception {
        String email = "user-wrong-pw-" + UUID.randomUUID() + "@stockflow.dev";
        mvc().perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, "Demo1234!"))))
            .andExpect(status().isCreated());

        mvc().perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, "NotMyPassword!"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("An unauthenticated request to a protected route returns 401")
    void unauthenticatedReturnsUnauthorized() throws Exception {
        mvc().perform(get("/api/products"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Issuing an invoice with more than available stock is rejected with 409")
    void invoiceOverStockIsRejected() throws Exception {
        String token = registerAndLogin("user-stock-" + UUID.randomUUID() + "@stockflow.dev");
        UUID productId = createProduct(token, "STK-001", 5, "1000.00");

        Map<String, Object> invoice = Map.of(
            "customerName", "Customer A",
            "items", List.of(Map.of("productId", productId, "quantity", 99))
        );

        mvc().perform(post("/api/invoices")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invoice)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("STOCK_INSUFFICIENT"))
            .andExpect(jsonPath("$.error.message").value(
                org.hamcrest.Matchers.containsString("Product STK-001")));
    }

    @Test
    @DisplayName("Issuing an invoice decrements stock atomically for every line")
    void issuingDecrementsStock() throws Exception {
        String token = registerAndLogin("user-issue-" + UUID.randomUUID() + "@stockflow.dev");
        UUID a = createProduct(token, "ISS-A", 10, "1000.00");
        UUID b = createProduct(token, "ISS-B", 7, "2000.00");

        Map<String, Object> invoice = Map.of(
            "customerName", "Customer B",
            "items", List.of(
                Map.of("productId", a, "quantity", 3),
                Map.of("productId", b, "quantity", 2)
            )
        );

        MvcResult created = mvc().perform(post("/api/invoices")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invoice)))
            .andExpect(status().isCreated())
            .andReturn();
        String invoiceId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mvc().perform(post("/api/invoices/" + invoiceId + "/issue")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ISSUED"));

        // Stock should now be 7 and 5.
        mvc().perform(get("/api/products/" + a).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantityOnHand").value(7));
        mvc().perform(get("/api/products/" + b).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantityOnHand").value(5));
    }

    @Test
    @DisplayName("Cancelling an ISSUED invoice restores the stock it consumed")
    void cancellingIssuedRestoresStock() throws Exception {
        String token = registerAndLogin("user-cancel-" + UUID.randomUUID() + "@stockflow.dev");
        UUID productId = createProduct(token, "CNL-001", 10, "1500.00");

        Map<String, Object> invoice = Map.of(
            "customerName", "Customer C",
            "items", List.of(Map.of("productId", productId, "quantity", 4))
        );

        MvcResult created = mvc().perform(post("/api/invoices")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invoice)))
            .andExpect(status().isCreated())
            .andReturn();
        String invoiceId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mvc().perform(post("/api/invoices/" + invoiceId + "/issue")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        mvc().perform(post("/api/invoices/" + invoiceId + "/cancel")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        mvc().perform(get("/api/products/" + productId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantityOnHand").value(10));
    }

    @Test
    @DisplayName("Illegal status transitions are rejected with 409")
    void illegalTransitionsAreRejected() throws Exception {
        String token = registerAndLogin("user-status-" + UUID.randomUUID() + "@stockflow.dev");
        UUID productId = createProduct(token, "STS-001", 5, "1000.00");

        Map<String, Object> invoice = Map.of(
            "customerName", "Customer D",
            "items", List.of(Map.of("productId", productId, "quantity", 1))
        );
        MvcResult created = mvc().perform(post("/api/invoices")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invoice)))
            .andExpect(status().isCreated())
            .andReturn();
        String invoiceId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        // DRAFT -> PAID is not allowed
        mvc().perform(post("/api/invoices/" + invoiceId + "/pay")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("Changing a product's price after an invoice is issued does not alter the invoice")
    void priceSnapshotIsPreserved() throws Exception {
        String token = registerAndLogin("user-price-" + UUID.randomUUID() + "@stockflow.dev");
        UUID productId = createProduct(token, "PRC-001", 10, "1000.00");

        Map<String, Object> invoice = Map.of(
            "customerName", "Customer E",
            "items", List.of(Map.of("productId", productId, "quantity", 2))
        );
        MvcResult created = mvc().perform(post("/api/invoices")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invoice)))
            .andExpect(status().isCreated())
            .andReturn();
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String invoiceId = body.get("id").asText();
        String oldSubtotal = body.get("subtotal").asText();

        // Change price
        ProductRequest update = new ProductRequest("PRC-001", "Product PRC-001", "Desc",
            new BigDecimal("9999.00"), 10);
        mvc().perform(post("/api/products")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)));

        MvcResult fetched = mvc().perform(get("/api/invoices/" + invoiceId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode invoiceAfter = objectMapper.readTree(fetched.getResponse().getContentAsString());
        assertThat(invoiceAfter.get("subtotal").asText()).isEqualTo(oldSubtotal);
        assertThat(invoiceAfter.get("items").get(0).get("unitPrice").decimalValue())
            .isEqualByComparingTo(new BigDecimal("1000.00"));
    }
}
