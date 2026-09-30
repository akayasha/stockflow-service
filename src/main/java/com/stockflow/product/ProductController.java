package com.stockflow.product;

import com.stockflow.common.page.PageResponse;
import com.stockflow.common.security.AuthenticatedUser;
import com.stockflow.common.security.CurrentUser;
import com.stockflow.product.dto.ProductRequest;
import com.stockflow.product.dto.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Shared inventory items.")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "List all products with pagination and search.")
    @GetMapping
    public PageResponse<ProductResponse> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "q", required = false) String q
    ) {
        AuthenticatedUser currentUser = CurrentUser.require();

        return productService.list(currentUser.id(), q, page, size)
                .map(ProductResponse::from);
    }

    @Operation(summary = "Get a single product by id.")
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable UUID id) {
        AuthenticatedUser currentUser = CurrentUser.require();

        return ProductResponse.from(
                productService.get(currentUser.id(), id)
        );
    }

    @Operation(summary = "Create a new product.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(
            @Valid @RequestBody ProductRequest request
    ) {
        AuthenticatedUser currentUser = CurrentUser.require();

        return ProductResponse.from(
                productService.create(currentUser.id(), request)
        );
    }

    @Operation(summary = "Update an existing product.")
    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request
    ) {
        AuthenticatedUser currentUser = CurrentUser.require();

        return ProductResponse.from(
                productService.update(currentUser.id(), id, request)
        );
    }

    @Operation(summary = "Delete a product owned by the current user.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        AuthenticatedUser currentUser = CurrentUser.require();

        productService.delete(currentUser.id(), id);

        return ResponseEntity.noContent().build();
    }
}
