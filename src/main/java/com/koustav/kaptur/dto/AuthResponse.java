package com.koustav.kaptur.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * DTO returned after successful authentication.
 * Contains the JWT token and safe user profile fields — does NOT embed
 * the full User entity to avoid leaking sensitive fields like password.
 */
@Data
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private UUID kptId;
    private String email;
    private String name;
    private String imageUrl;
}
