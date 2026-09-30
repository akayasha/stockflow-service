package com.stockflow.auth;

import com.stockflow.auth.dto.*;
import com.stockflow.common.error.ConflictException;
import com.stockflow.common.error.UnauthorizedException;
import com.stockflow.common.security.JwtService;
import com.stockflow.user.Role;
import com.stockflow.user.User;
import com.stockflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public UserSummary register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("EMAIL_TAKEN", "Email is already registered");
        }
        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.STAFF);
        user.setTokenVersion(0L);
        User saved = userRepository.save(user);
        log.info("Registered new user {}", saved.getEmail());
        return new UserSummary(saved.getId(), saved.getEmail(), saved.getRole().name());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Always call the encoder so the response time does not depend on
        // whether the user exists. If the user does not exist we hash a dummy
        // password first; otherwise BCrypt.compare runs against the real hash.
        User user = userRepository.findByEmailIgnoreCase(request.email()).orElse(null);
        String hashToCompare = user != null
            ? user.getPasswordHash()
            : passwordEncoder.encode("not-a-real-password");
        boolean matches = passwordEncoder.matches(request.password(), hashToCompare);

        if (user == null || !matches) {
            throw new UnauthorizedException();
        }

        String token = jwtService.issue(user);
        Instant expiresAt = jwtService.expirationFor(user);
        return new AuthResponse(user.getId(), user.getEmail(), user.getRole().name(), token, expiresAt);
    }

    @Transactional
    public void logout(java.util.UUID userId) {
        // Bumping tokenVersion invalidates every JWT previously issued to this
        // user without us maintaining a blacklist table.
        userRepository.findById(userId).ifPresent(user -> {
            user.setTokenVersion(user.getTokenVersion() + 1);
            userRepository.save(user);
        });
    }

    @Transactional(readOnly = true)
    public AuthResponse loginAdmin(AdminLoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email()).orElse(null);

        String hashToCompare = user != null
                ? user.getPasswordHash()
                : passwordEncoder.encode("not-a-real-password");

        boolean matches = passwordEncoder.matches(request.password(), hashToCompare);

        if (user == null || !matches || user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException();
        }

        String token = jwtService.issue(user);
        Instant expiresAt = jwtService.expirationFor(user);

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                token,
                expiresAt
        );
    }

}
