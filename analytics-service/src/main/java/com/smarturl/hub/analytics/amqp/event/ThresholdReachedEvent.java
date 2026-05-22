package com.smarturl.hub.analytics.amqp.event;

import java.time.Instant;
import java.util.UUID;

public record ThresholdReachedEvent(
        UUID eventId,
        UUID linkId,
        UUID webhookConfigId,
        long threshold,
        long currentCount,
        Instant reachedAt) {
}
