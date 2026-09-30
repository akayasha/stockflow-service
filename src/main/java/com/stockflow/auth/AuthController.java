package com.stockflow.auth;

import com.stockflow.auth.dto.AuthResponse;
import com.stockflow.auth.dto.LoginRequest;
import com.stockflow.auth.dto.RegisterRequest;
import com.stockflow.auth.dto.UserSummary;
import com.stockflow.common.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.stockflow.auth.dto.AdminLoginRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserSummary> register(@Valid @RequestBody RegisterRequest request) {
        UserSummary summary = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout(CurrentUser.require().id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login-admin")
    public ResponseEntity<AuthResponse> loginAdmin(
            @Valid @RequestBody AdminLoginRequest request
    ) {
        return ResponseEntity.ok(authService.loginAdmin(request));
    }
}
