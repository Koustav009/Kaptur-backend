package com.koustav.kaptur.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for requesting a new Access Token using an existing valid Refresh Token.
 * @JsonAlias allows clients to send either "refreshToken" or "refresh_token" in JSON.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token cannot be blank")
    @JsonAlias({"refresh_token", "refreshToken"})
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }
}
