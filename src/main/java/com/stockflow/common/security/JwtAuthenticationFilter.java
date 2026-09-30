package com.stockflow.common.security;

import com.stockflow.user.User;
import com.stockflow.user.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inspects the {@code Authorization: Bearer ...} header on every request,
 * validates the JWT and - when valid - sets the authenticated principal on the
 * security context. The token's {@code tv} claim is compared against the
 * current user record; a mismatch is treated as a logged-out token.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
        throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIX.length()).trim();
        Optional<Claims> claims = jwtService.parse(token);
        if (claims.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            UUID userId = UUID.fromString(claims.get().getSubject());
            long tokenVersionInClaim = claims.get().get(JwtService.CLAIM_TOKEN_VERSION, Long.class);
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty() || userOpt.get().getTokenVersion() != tokenVersionInClaim) {
                // Token references a user that no longer exists, or its
                // version is stale - silently treat as anonymous.
                chain.doFilter(request, response);
                return;
            }
            User user = userOpt.get();
            AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(), user.getEmail(), user.getRole().name());
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (IllegalArgumentException ex) {
            log.debug("Malformed JWT subject: {}", ex.getMessage());
        }

        chain.doFilter(request, response);
    }
}
