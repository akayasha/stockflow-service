package com.stockflow.common.security;

import com.stockflow.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and verifies JWT access tokens.
 *
 * <p>Tokens are signed with HS256 using the application secret. Each token
 * carries the user's {@code tokenVersion} claim. The auth filter compares the
 * claim against the value currently stored on the user record; if they differ,
 * the token is rejected even if it is otherwise valid. This makes logout work
 * without a server-side blacklist - incrementing {@code tokenVersion} instantly
 * invalidates every previously issued token for that user.
 */
@Slf4j
@Service
public class JwtService {

    public static final String CLAIM_TOKEN_VERSION = "tv";
    public static final String CLAIM_EMAIL = "email";

    private final JwtProperties props;
    private final SecretKey signingKey;

    public JwtService(JwtProperties props) {
        this.props = props;
        if (props.secret() == null || props.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "JWT secret must be configured and at least 32 bytes long for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Issue a fresh access token for the given user.
     */
    public String issue(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(props.expiration());
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim(CLAIM_EMAIL, user.getEmail())
            .claim(CLAIM_TOKEN_VERSION, user.getTokenVersion())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .id(UUID.randomUUID().toString())
            .signWith(signingKey)
            .compact();
    }

    /**
     * Parse and validate a token. Returns the claims when signature, expiry
     * and basic shape are valid; an empty optional if anything is wrong.
     */
    public Optional<Claims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT parse failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Instant expirationFor(User user) {
        return Instant.now().plus(props.expiration());
    }
}
