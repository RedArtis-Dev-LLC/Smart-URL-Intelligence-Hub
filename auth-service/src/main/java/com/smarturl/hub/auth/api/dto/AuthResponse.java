package com.smarturl.hub.auth.api.dto;

import com.smarturl.hub.auth.service.JwtService.IssuedAccessToken;
import com.smarturl.hub.auth.service.RefreshTokenService.IssuedRefreshToken;
import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt) {

    public static AuthResponse of(IssuedAccessToken access, IssuedRefreshToken refresh) {
        return new AuthResponse(
                access.token(),
                refresh.token(),
                "Bearer",
                access.expiresAt(),
                refresh.expiresAt());
    }
}
