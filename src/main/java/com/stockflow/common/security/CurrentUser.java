package com.stockflow.common.security;

import com.stockflow.common.error.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthenticatedUser require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new UnauthorizedException("Authentication required");
        }

        return principal;
    }

    public static AuthenticatedUser requireAdmin() {
        AuthenticatedUser user = require();

        if (!"ADMIN".equalsIgnoreCase(user.role())) {
            throw new UnauthorizedException("Admin role required");
        }

        return user;
    }

    public static AuthenticatedUser orNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        if (auth.getPrincipal() instanceof AuthenticatedUser principal) {
            return principal;
        }

        return null;
    }
}