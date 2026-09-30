package com.stockflow.common.base;

import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves the current auditor (the user making the change) for JPA auditing.
 *
 * <p>Returns the authenticated user's email so {@code created_by} and
 * {@code updated_by} columns hold something a human can read in the database.
 * Falls back to {@code system} when no authentication is present - this
 * happens during application bootstrapping (Flyway seed runners, etc.).
 */
@Component("auditorAware")
public class AuditorAwareImpl implements AuditorAware<String> {

    private static final String SYSTEM = "system";

    @NonNull
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of(SYSTEM);
        }
        String name = authentication.getName();
        // Anonymous filter chain produces an "anonymousUser" principal; treat
        // it the same as no authentication.
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return Optional.of(SYSTEM);
        }
        return Optional.of(name);
    }
}
