package com.smarturl.hub.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "analytics")
public record AnalyticsProperties(
        Amqp amqp,
        Geo geo,
        WebhookService webhookService,
        Redis redis) {

    public record Amqp(
            String clickEventsExchange,
            String clickEventsRoutingKey,
            String clickEventsQueue,
            String clickEventsDlq,
            String thresholdExchange,
            String thresholdRoutingKey) {
    }

    public record Geo(String baseUrl) {
    }

    public record WebhookService(String baseUrl) {
    }

    public record Redis(String clickTotalPrefix, String clickUniquePrefix) {
    }
}
