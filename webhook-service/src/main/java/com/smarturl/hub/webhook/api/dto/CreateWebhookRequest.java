package com.smarturl.hub.webhook.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import org.hibernate.validator.constraints.URL;

public record CreateWebhookRequest(
        @NotNull UUID linkId,
        @NotNull @URL String targetUrl,
        @Positive long threshold) {
}
