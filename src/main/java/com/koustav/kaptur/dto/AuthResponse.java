package com.koustav.kaptur.dto;

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
    private String googleId;
    private String email;
    private String name;
    private String picture;

    // public AuthResponse(String accessToken) {
    //     this.accessToken = accessToken;
    // }
}
