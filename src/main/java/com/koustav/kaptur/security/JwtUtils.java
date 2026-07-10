package com.koustav.kaptur.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.koustav.kaptur.model.CustomUserDetails;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * @Component makes this class available as a "Bean" to other classes. This
 *            class handles all JWT (JSON Web Token) creation and validation.
 */
@Slf4j
@Component
public class JwtUtils {

    // Reads the secret key from application.properties or uses the default.
    @Value("${app.jwtSecret:9a4f2c8d3b7a1e5f8g2h6i0j4k9l3m7n5o1p4q8r2s6t0u4v8w2x6y0z}")
    private String jwtSecret;

    // Reads how long the token is valid (86400000ms = 1 day).
    @Value("${app.jwtExpirationMs:86400000}")
    private int jwtExpirationMs;

    // Creates a signing key based on our secret string.
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private String buildJwt(String KptId) {
        log.debug("Building JWT for kptId: {}", KptId);
        return Jwts.builder().subject(KptId) // Sets the KPT ID in the token
                .issuedAt(new Date()) // Token creation time
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs)) // Expiry time
                .signWith(getSigningKey()) // Signs the token with our secret
                .compact();
    }

    /**
     * Generates a token for a user after they log in.
     */
    public String generateJwtToken(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        log.debug("Generating JWT for authenticated user: {}", userPrincipal.getKptId());
        return buildJwt(userPrincipal.getKptId().toString());
    }

    /**
     * Used for generating tokens for Google OAuth users.
     */
    public String generateTokenFromKptId(String KptId) {
        log.debug("Generating JWT from kptId: {}", KptId);
        return buildJwt(KptId);
    }

    /**
     * Decodes the token to get the user's email.
     */
    public String getUserNameFromJwtToken(String token) {
        String kptId = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token)
                .getPayload().getSubject();
        log.debug("Extracted kptId from JWT: {}", kptId);
        return kptId;
    }

    /**
     * Checks if the token is valid (not expired, signature matches, etc.).
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            log.debug("JWT validation successful");
            return true;
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
