package com.worklink.security;

import com.worklink.entity.User;
import com.worklink.entity.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

/**
 * Owns JWT creation and cryptographic parsing. Controllers and domain services do not handle JWT details.
 */
@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = createSigningKey(properties.secret());
    }

    public String generateAccessToken(User user) {
        Instant issuedAt = Instant.now();
        return generateAccessToken(user, issuedAt, issuedAt.plusMillis(properties.accessTokenExpirationMs()));
    }

    /**
     * Exposed for deterministic token tests; production callers use {@link #generateAccessToken(User)}.
     */
    public String generateAccessToken(User user, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return parseAndValidate(token).getSubject();
    }

    public Role extractRole(String token) {
        String role = parseAndValidate(token).get("role", String.class);
        return Role.valueOf(role);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseAndValidate(token);
            return claims.getSubject() != null && claims.getExpiration() != null;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private SecretKey createSigningKey(String encodedSecret) {
        try {
            // Secrets are often supplied by environment managers in URL-safe Base64
            // form (using '-' and '_' instead of '+' and '/'). Normalize that form
            // so both standard and URL-safe Base64 values work consistently.
            String normalizedSecret = encodedSecret
                    .replace('-', '+')
                    .replace('_', '/');
            byte[] keyBytes = Decoders.BASE64.decode(normalizedSecret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("JWT_SECRET must be a valid Base64-encoded key of at least 256 bits", exception);
        }
    }
}
