package com.skillcircle.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for refreshing an access token.
 */
@Data
public class TokenRefreshRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
