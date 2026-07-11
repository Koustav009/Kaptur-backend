package com.koustav.kaptur.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO returned after successful authentication and token refresh.
 * Contains both Access Token (short-lived) and Refresh Token (long-lived),
 * plus safe user profile fields (including the user's latest DB role).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private UUID kptId;
    private String email;
    private String name;
    private String imageUrl;
    private String role;

    // NOTE FOR LEARNERS:
    // By adding these getters with @JsonProperty, when our backend converts this object to JSON,
    // it outputs both "accessToken" and "access_token" (and "refreshToken" and "refresh_token").
    // This guarantees compatibility with any frontend (Flutter/React) regardless of whether
    // they expect camelCase or snake_case keys!
    @JsonProperty("access_token")
    public String getAccess_token() {
        return accessToken;
    }

    @JsonProperty("refresh_token")
    public String getRefresh_token() {
        return refreshToken;
    }
}
