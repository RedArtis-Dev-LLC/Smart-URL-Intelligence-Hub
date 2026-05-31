package com.smarturl.hub.analytics.amqp.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClickEventMessage(
        UUID eventId,
        UUID linkId,
        UUID userId,
        String shortCode,
        String originalUrl,
        Instant timestamp,
        String ip,
        String userAgent,
        String referrer) {
}
