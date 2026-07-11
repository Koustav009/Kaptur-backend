package com.koustav.kaptur.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Entity marks this class as a JPA entity that maps to the REFRESH_TOKENS table in PostgreSQL.
 * 
 * WHY REFRESH TOKENS?
 * In a secure JWT authentication architecture, we use two tokens:
 * 1. Access Token: Short-lived (e.g., 15 minutes) used to authenticate every API request.
 * 2. Refresh Token: Long-lived (e.g., 7 days) used ONLY to obtain a new Access Token when the old one expires.
 * 
 * By storing the Refresh Token in our database, we can:
 * - Verify that the refresh token is legitimate and hasn't been revoked.
 * - Perform a live check against the database (`check and update the role from db`) when issuing a new access token.
 * - Easily revoke access if the user logs out or if their credentials are compromised.
 */
@Entity
@Table(name = "REFRESH_TOKENS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    // Primary key for the refresh token table (UUID v7 time-ordered)
    @Id
    @Column(name = "TOKEN_ID", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID tokenId;

    // @OneToOne links this refresh token to exactly one User entity.
    // Each user will have their current active refresh token associated here.
    @OneToOne
    @JoinColumn(name = "KPT_ID", referencedColumnName = "KPT_ID", nullable = false)
    private User user;

    // The unique string representation of the refresh token sent to the client.
    @Column(nullable = false, unique = true)
    private String token;

    // The timestamp when this refresh token expires.
    // We check this when the user requests a new access token.
    @Column(nullable = false)
    private Instant expiryDate;
}
