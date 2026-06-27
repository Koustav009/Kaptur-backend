package com.koustav.kaptur.dto;

import com.koustav.kaptur.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private User user;

    // public AuthResponse(String accessToken) {
    // this.accessToken = accessToken;
    // }
}
