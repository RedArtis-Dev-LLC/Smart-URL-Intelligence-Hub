package com.smarturl.hub.link.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "link")
public record LinkProperties(
        ShortCode shortCode,
        Redirect redirect,
        Outbox outbox,
        Amqp amqp) {

    public record ShortCode(int length, int maxGenerationRetries) {
    }

    public record Redirect(int maxOptimisticLockRetries) {
    }

    public record Outbox(long pollIntervalMs, int batchSize, String cleanupCron, Duration retentionPeriod) {
    }

    public record Amqp(String clickEventsExchange, String clickEventsRoutingKey) {
    }
}
