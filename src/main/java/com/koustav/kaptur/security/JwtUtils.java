package com.koustav.kaptur.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.koustav.kaptur.model.CustomUserDetails;
import com.koustav.kaptur.model.User;
import com.koustav.kaptur.model.enums.SystemRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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

    private String buildJwt(String kptId, String role) {
        log.debug("Building JWT access token for kptId: {} with role: {}", kptId, role);
        return Jwts.builder().subject(kptId) // Sets the KPT ID in the token
                .claim("role", role != null ? role : SystemRole.USER.name()) // Embeds the user's system role in the token
                .issuedAt(new Date()) // Token creation time
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs)) // Expiry time
                .signWith(getSigningKey()) // Signs the token with our secret
                .compact();
    }

    private String buildJwt(String kptId) {
        return buildJwt(kptId, SystemRole.USER.name());
    }

    /**
     * Generates a token for a user after they log in.
     */
    public String generateJwtToken(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        log.debug("Generating JWT for authenticated user: {}", userPrincipal.getKptId());
        String roleStr = userPrincipal.getUser().getRole() != null ? userPrincipal.getUser().getRole().name() : SystemRole.USER.name();
        return buildJwt(userPrincipal.getKptId().toString(), roleStr);
    }

    /**
     * Generates a new access token directly from a User entity.
     * Used during OAuth login and Refresh Token flows when renewing the access token with the latest DB role.
     */
    public String generateTokenFromUser(User user) {
        log.debug("Generating JWT from User entity kptId: {}", user.getKptId());
        String roleStr = user.getRole() != null ? user.getRole().name() : SystemRole.USER.name();
        return buildJwt(user.getKptId().toString(), roleStr);
    }

    /**
     * Used for generating tokens for Google OAuth users or KPT IDs directly.
     */
    public String generateTokenFromKptId(String kptId) {
        log.debug("Generating JWT from kptId: {}", kptId);
        return buildJwt(kptId, SystemRole.USER.name());
    }

    /**
     * Decodes the token to get the user's KPT ID (Subject).
     */
    public String getUserNameFromJwtToken(String token) {
        String kptId = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token)
                .getPayload().getSubject();
        log.debug("Extracted kptId from JWT: {}", kptId);
        return kptId;
    }

    /**
     * Decodes the token to get the user's role claim.
     */
    public String getRoleFromJwtToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
            Object roleObj = claims.get("role");
            if (roleObj != null) {
                return roleObj.toString();
            }
        } catch (Exception e) {
            log.debug("Could not extract role claim from JWT: {}", e.getMessage());
        }
        return SystemRole.USER.name();
    }

    /**
     * Checks if the token is valid (signature matches AND not expired).
     * This is stateless verification — NO database queries are executed here.
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

    /**
     * Checks ONLY the cryptographic signature of the JWT token (does not query DB and does not fail on expiry).
     * Useful when checking signature integrity before triggering a token refresh.
     */
    public boolean validateJwtSignatureOnly(String authToken) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            return true;
        } catch (ExpiredJwtException e) {
            // Even if expired, Jwts.parser() successfully verified the signature before throwing ExpiredJwtException!
            log.debug("JWT signature is valid (though token is expired)");
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return false;
        }
    }
}
