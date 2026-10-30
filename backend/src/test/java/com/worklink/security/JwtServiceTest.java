package com.worklink.security;

import com.worklink.entity.User;
import com.worklink.entity.enums.AccountStatus;
import com.worklink.entity.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtServiceTest {

    @Test
    void acceptsUrlSafeBase64Secret() {
        // 32 bytes encoded with the URL-safe Base64 alphabet (contains '_').
        JwtProperties properties = new JwtProperties("__________________________________________8=", 3_600_000);
        JwtService service = new JwtService(properties);
        User user = new User("Test", "User", "user@example.com", "password", Role.FREELANCER, AccountStatus.ACTIVE);

        String token = service.generateAccessToken(user, Instant.now(), Instant.now().plusSeconds(60));

        assertEquals("user@example.com", service.extractEmail(token));
    }
}
