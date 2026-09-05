package com.worklink.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        @NotBlank(message = "JWT secret must be configured") String secret,
        @Positive(message = "JWT access-token expiration must be positive") long accessTokenExpirationMs
) {
}
