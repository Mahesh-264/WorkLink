package com.worklink.dto.auth;

import com.worklink.entity.enums.Role;

public record AuthUserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role
) {
}
