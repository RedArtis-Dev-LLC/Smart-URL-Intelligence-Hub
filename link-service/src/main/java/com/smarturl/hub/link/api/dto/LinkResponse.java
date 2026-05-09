package com.smarturl.hub.link.api.dto;

import com.smarturl.hub.link.domain.Link;
import java.time.Instant;
import java.util.UUID;

public record LinkResponse(
        UUID id,
        UUID userId,
        String originalUrl,
        String shortCode,
        String customSlug,
        Instant expiresAt,
        Long maxClicks,
        long clickCount,
        boolean active,
        Instant createdAt) {

    public static LinkResponse from(Link link) {
        return new LinkResponse(
                link.getId(),
                link.getUserId(),
                link.getOriginalUrl(),
                link.getShortCode(),
                link.getCustomSlug(),
                link.getExpiresAt(),
                link.getMaxClicks(),
                link.getClickCount(),
                link.isActive(),
                link.getCreatedAt());
    }
}
