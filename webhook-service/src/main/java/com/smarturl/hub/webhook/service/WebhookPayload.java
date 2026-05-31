package com.smarturl.hub.webhook.service;

import java.time.Instant;
import java.util.UUID;

public record WebhookPayload(
        UUID eventId,
        UUID linkId,
        String shortCode,
        String originalUrl,
        long threshold,
        long currentCount,
        Instant reachedAt) {
}
