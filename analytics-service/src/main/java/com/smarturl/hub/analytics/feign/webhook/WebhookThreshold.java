package com.smarturl.hub.analytics.feign.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookThreshold(UUID configId, long threshold) {
}
