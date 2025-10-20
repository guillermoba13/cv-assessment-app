package com.cv.review.service.cvservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple JWT provider (HS256) for:
 * - validating incoming tokens
 * - extracting user and roles (claims)
 * - generating test tokens (utility method)
 *
 * In production, if your login service signs with RSA/JWK, replace the logic with a JwtDecoder based on the public key/JWKS.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final Key signingKey;
    private final long jwtExpirationMs;

    public JwtTokenProvider(@Value("${cvreview.security.jwt-secret}") String jwtSecret,
                            @Value("${cvreview.security.jwt-expiration-ms:3600000}") long jwtExpirationMs) {
        // Convenience: generate HMAC key from the configured secret
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalArgumentException("cvreview.security.jwt-secret no puede estar vacío");
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Validates a JWT token (signature and expiration).
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Token JWT inválido: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Extract the subject (username) from the token.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(signingKey).build()
                .parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    /**
     * Extract roles from the “roles” claim (if included). Expects a list of strings.
     */
    // @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(signingKey).build()
                .parseClaimsJws(token).getBody();
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof Collection) {
            return ((Collection<?>) rolesObj).stream().map(Object::toString).collect(Collectors.toList());
        }
        if (rolesObj != null) {
            return List.of(rolesObj.toString());
        }
        return Collections.emptyList();
    }

    /**
     * Generates a test JWT token (HS256). Useful for development/local testing.
     * Should not be used in production without checking expiration and claims.
     */
    public String generateToken(String username, List<String> roles) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiry = new Date(now + jwtExpirationMs);

        Claims claims = Jwts.claims().setSubject(username);
        if (roles != null && !roles.isEmpty()) {
            claims.put("roles", roles);
        }

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(issuedAt)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
