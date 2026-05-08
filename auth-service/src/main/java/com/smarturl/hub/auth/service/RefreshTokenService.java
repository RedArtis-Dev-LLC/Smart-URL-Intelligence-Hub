package com.smarturl.hub.auth.service;

import com.smarturl.hub.auth.config.JwtProperties;
import com.smarturl.hub.auth.domain.RefreshToken;
import com.smarturl.hub.auth.domain.RefreshTokenRepository;
import com.smarturl.hub.auth.error.TokenInvalidException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;
    private final Clock clock;

    @Transactional
    public IssuedRefreshToken issue(UUID userId) {
        String raw = UUID.randomUUID().toString();
        Instant now = clock.instant();
        RefreshToken stored = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(hash(raw))
                .expiresAt(now.plus(properties.refreshTokenTtl()))
                .revoked(false)
                .createdAt(now)
                .build();
        repository.save(stored);
        return new IssuedRefreshToken(raw, stored.getExpiresAt());
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        RefreshToken existing = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new TokenInvalidException("Refresh token not recognised"));

        if (existing.isRevoked()) {
            throw new TokenInvalidException("Refresh token has been revoked");
        }
        if (existing.getExpiresAt().isBefore(clock.instant())) {
            throw new TokenInvalidException("Refresh token has expired");
        }

        existing.setRevoked(true);
        repository.save(existing);

        IssuedRefreshToken next = issue(existing.getUserId());
        return new RotationResult(existing.getUserId(), next);
    }

    static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record IssuedRefreshToken(String token, Instant expiresAt) {}

    public record RotationResult(UUID userId, IssuedRefreshToken next) {}
}
