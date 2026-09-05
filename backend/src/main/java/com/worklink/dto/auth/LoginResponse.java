package com.worklink.dto.auth;

import com.worklink.entity.enums.Role;

/**
 * Safe login result. The access token is the only credential returned to the client.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role
) {
}
