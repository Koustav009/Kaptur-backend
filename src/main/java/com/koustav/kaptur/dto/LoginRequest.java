package com.koustav.kaptur.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for email/password login requests.
 */
@Data
public class LoginRequest {
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
