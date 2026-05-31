package com.smarturl.hub.webhook.api.dto;

import com.smarturl.hub.webhook.domain.WebhookConfig;
import java.time.Instant;
import java.util.UUID;

public record WebhookConfigResponse(
        UUID id,
        UUID linkId,
        String targetUrl,
        long threshold,
        boolean active,
        Instant createdAt) {

    public static WebhookConfigResponse from(WebhookConfig config) {
        return new WebhookConfigResponse(
                config.getId(),
                config.getLinkId(),
                config.getTargetUrl(),
                config.getThreshold(),
                config.isActive(),
                config.getCreatedAt());
    }
}
