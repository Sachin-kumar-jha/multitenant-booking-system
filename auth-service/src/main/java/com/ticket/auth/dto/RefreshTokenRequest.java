package com.ticket.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for token refresh.
 */
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
