package com.smarturl.hub.analytics.service;

import com.smarturl.hub.analytics.amqp.ThresholdReachedPublisher;
import com.smarturl.hub.analytics.amqp.event.ThresholdReachedEvent;
import com.smarturl.hub.analytics.feign.webhook.WebhookThreshold;
import com.smarturl.hub.analytics.feign.webhook.WebhookThresholdsClient;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThresholdDetector {

    private final WebhookThresholdsClient webhookThresholdsClient;
    private final ThresholdReachedPublisher publisher;
    private final Clock clock;

    public void check(UUID linkId, long currentCount) {
        List<WebhookThreshold> thresholds = fetchThresholds(linkId);
        if (thresholds.isEmpty()) {
            return;
        }
        for (WebhookThreshold threshold : thresholds) {
            if (threshold.threshold() == currentCount) {
                publisher.publish(new ThresholdReachedEvent(
                        UUID.randomUUID(),
                        linkId,
                        threshold.configId(),
                        threshold.threshold(),
                        currentCount,
                        Instant.now(clock)));
            }
        }
    }

    private List<WebhookThreshold> fetchThresholds(UUID linkId) {
        try {
            List<WebhookThreshold> result = webhookThresholdsClient.findByLinkId(linkId);
            return result == null ? List.of() : result;
        } catch (RuntimeException ex) {
            log.warn("Failed to fetch webhook thresholds for linkId={}: {}", linkId, ex.getMessage());
            return List.of();
        }
    }
}
