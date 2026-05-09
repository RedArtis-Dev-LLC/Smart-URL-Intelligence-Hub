package com.smarturl.hub.link.service;

import java.time.Instant;
import java.util.UUID;

public record ClickEventPayload(
        UUID eventId,
        UUID linkId,
        UUID userId,
        String shortCode,
        Instant timestamp,
        String ip,
        String userAgent,
        String referrer) {
}
