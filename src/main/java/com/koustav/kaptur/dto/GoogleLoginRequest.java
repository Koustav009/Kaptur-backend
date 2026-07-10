package com.koustav.kaptur.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for Google native login requests.
 * Receives the Google ID token from the Flutter app for server-side verification.
 */
@Data
public class GoogleLoginRequest {
    @NotBlank(message = "Google ID token is required")
    private String id;
}
