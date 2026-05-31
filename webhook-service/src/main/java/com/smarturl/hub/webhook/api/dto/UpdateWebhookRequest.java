package com.smarturl.hub.webhook.api.dto;

import org.hibernate.validator.constraints.URL;

public record UpdateWebhookRequest(
        @URL String targetUrl,
        Boolean active) {
}
