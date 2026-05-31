package com.smarturl.hub.webhook.api.dto;

import java.util.UUID;

public record WebhookThresholdResponse(UUID configId, long threshold) {
}
