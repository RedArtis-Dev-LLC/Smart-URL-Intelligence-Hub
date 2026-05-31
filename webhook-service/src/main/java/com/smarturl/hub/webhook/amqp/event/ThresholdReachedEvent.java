package com.smarturl.hub.webhook.amqp.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ThresholdReachedEvent(
        UUID eventId,
        UUID linkId,
        String shortCode,
        String originalUrl,
        UUID webhookConfigId,
        long threshold,
        long currentCount,
        Instant reachedAt) {
}
