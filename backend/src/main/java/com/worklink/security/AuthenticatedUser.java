package com.worklink.security;

import com.worklink.entity.enums.Role;

/**
 * Immutable authenticated principal derived from a verified token and current database user.
 */
public record AuthenticatedUser(Long id, String email, Role role) {
}
