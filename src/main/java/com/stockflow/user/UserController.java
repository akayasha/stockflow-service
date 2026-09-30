package com.stockflow.user;

import com.stockflow.common.security.CurrentUser;
import com.stockflow.user.dto.UpdateRoleRequest;
import com.stockflow.auth.dto.UserSummary;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserSummary> updateRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        CurrentUser.requireAdmin();

        return ResponseEntity.ok(
                userService.updateRole(userId, request.role())
        );
    }
}