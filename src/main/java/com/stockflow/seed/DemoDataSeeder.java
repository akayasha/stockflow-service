package com.stockflow.seed;

import com.stockflow.product.Product;
import com.stockflow.product.ProductRepository;
import com.stockflow.user.Role;
import com.stockflow.user.User;
import com.stockflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    public static final String DEMO_EMAIL = "demo@stockflow.dev";
    public static final String DEMO_PASSWORD = "Demo1234!";

    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Value("${stockflow.app.seed-demo-data:true}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Demo seeding disabled (stockflow.app.seed-demo-data=false)");
            return;
        }

        User demoUser = userRepository.findByEmailIgnoreCase(DEMO_EMAIL)
                .filter(user -> user.getRole() == Role.ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "Seed admin user is missing. Check Flyway migration V5__seed_admin_user.sql"
                ));

        seedProduct(
                demoUser.getId(),
                "BK-001",
                "Notebook A5",
                "Hardcover notebook, 80 pages.",
                new BigDecimal("35000.00"),
                50
        );

        seedProduct(
                demoUser.getId(),
                "BK-002",
                "Pocket Book",
                "Compact softcover notebook.",
                new BigDecimal("12000.00"),
                120
        );

        seedProduct(
                demoUser.getId(),
                "PC-001",
                "Ballpoint Pen",
                "Blue ink, medium tip.",
                new BigDecimal("4500.00"),
                200
        );

        seedProduct(
                demoUser.getId(),
                "PC-002",
                "Highlighter",
                "Yellow chisel-tip.",
                new BigDecimal("8000.00"),
                75
        );

        seedProduct(
                demoUser.getId(),
                "OF-001",
                "Sticky Notes Pack",
                "3x3 inch, 100 sheets.",
                new BigDecimal("15000.00"),
                0
        );
    }

    private void seedProduct(
            UUID ownerId,
            String sku,
            String name,
            String description,
            BigDecimal price,
            int qty
    ) {
        if (productRepository.existsByOwnerIdAndSkuIgnoreCase(ownerId, sku)) {
            return;
        }

        Product product = new Product();
        product.setOwnerId(ownerId);
        product.setSku(sku);
        product.setName(name);
        product.setDescription(description);
        product.setUnitPrice(price);
        product.setQuantityOnHand(qty);

        productRepository.save(product);

        log.info("Seeded product {} ({})", sku, name);
    }
}
