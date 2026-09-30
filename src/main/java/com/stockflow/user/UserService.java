package com.stockflow.user;

import com.stockflow.auth.dto.UserSummary;
import com.stockflow.common.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserSummary updateRole(UUID userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        user.setRole(role);

        User saved = userRepository.save(user);

        return new UserSummary(
                saved.getId(),
                saved.getEmail(),
                saved.getRole().name()
        );
    }
}