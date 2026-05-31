package com.smarturl.hub.webhook.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "webhook")
public record WebhookProperties(
        Amqp amqp,
        LinkService linkService,
        Delivery delivery) {

    public record Amqp(
            String thresholdExchange,
            String thresholdRoutingKey,
            String thresholdQueue,
            String thresholdDlq) {
    }

    public record LinkService(String baseUrl) {
    }

    public record Delivery(
            int maxAttempts,
            long initialBackoffMs,
            int backoffMultiplier) {
    }
}
