package com.worklink.controller;

import com.worklink.entity.enums.Role;
import com.worklink.security.AuthenticatedUser;
import com.worklink.security.CurrentUserProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Isolated development/testing endpoints. Disabled unless worklink.testing-endpoints.enabled is true.
 */
@RestController
@RequestMapping("/api/test")
@ConditionalOnProperty(prefix = "worklink.testing-endpoints", name = "enabled", havingValue = "true")
public class TestingAuthController {

    private final CurrentUserProvider currentUserProvider;

    public TestingAuthController(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/authenticated")
    public CurrentUserResponse authenticated() {
        return CurrentUserResponse.from(currentUserProvider.requireCurrentUser());
    }

    @GetMapping("/client-only")
    @PreAuthorize("hasRole('CLIENT')")
    public CurrentUserResponse clientOnly() {
        return CurrentUserResponse.from(currentUserProvider.requireCurrentUser());
    }

    public record CurrentUserResponse(Long id, String email, Role role) {
        static CurrentUserResponse from(AuthenticatedUser user) {
            return new CurrentUserResponse(user.id(), user.email(), user.role());
        }
    }
}
