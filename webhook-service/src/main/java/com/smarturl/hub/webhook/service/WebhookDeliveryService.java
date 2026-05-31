package com.smarturl.hub.webhook.service;

import com.smarturl.hub.webhook.amqp.event.ThresholdReachedEvent;
import com.smarturl.hub.webhook.config.WebhookProperties;
import com.smarturl.hub.webhook.domain.DeliveryStatus;
import com.smarturl.hub.webhook.domain.WebhookConfig;
import com.smarturl.hub.webhook.domain.WebhookConfigRepository;
import com.smarturl.hub.webhook.domain.WebhookDelivery;
import com.smarturl.hub.webhook.domain.WebhookDeliveryRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDeliveryService {

    static final String METRIC_NAME = "webhook.deliveries.total";
    static final String TAG_STATUS = "status";

    private final WebhookConfigRepository configRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookProperties properties;
    private final WebhookHttpClient httpClient;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public void deliver(ThresholdReachedEvent event) {
        if (deliveryRepository.existsByEventId(event.eventId())) {
            log.debug("Duplicate delivery skipped [eventId={}]", event.eventId());
            return;
        }

        var config = configRepository.findById(event.webhookConfigId()).orElse(null);
        if (config == null || !config.isActive()) {
            log.warn("Webhook config not found or inactive [configId={}]", event.webhookConfigId());
            return;
        }

        attemptDelivery(event, config);
    }

    private void attemptDelivery(ThresholdReachedEvent event, WebhookConfig config) {
        int maxAttempts = properties.delivery().maxAttempts();
        long backoff = properties.delivery().initialBackoffMs();
        int multiplier = properties.delivery().backoffMultiplier();

        String lastError = null;
        Integer responseCode = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                responseCode = httpClient.deliver(config.getTargetUrl(), event);
                if (responseCode >= 200 && responseCode < 300) {
                    persistDelivery(event, config, DeliveryStatus.SUCCESS, responseCode, attempt, null);
                    counter("success").increment();
                    return;
                }
                lastError = "HTTP " + responseCode;
            } catch (CallNotPermittedException _) {
                persistDelivery(event, config, DeliveryStatus.FAILED, null, attempt, "Circuit breaker open");
                counter("circuit_open").increment();
                return;
            } catch (RuntimeException ex) {
                lastError = ex.getMessage();
            }

            if (attempt < maxAttempts) {
                sleep(backoff);
                backoff *= multiplier;
            }
        }

        persistDelivery(event, config, DeliveryStatus.FAILED, responseCode, maxAttempts, lastError);
        counter("failed").increment();
    }

    private void persistDelivery(ThresholdReachedEvent event, WebhookConfig config,
                                 DeliveryStatus status, Integer responseCode,
                                 int attemptCount, String lastError) {
        var delivery = WebhookDelivery.builder()
                .id(UUID.randomUUID())
                .configId(config.getId())
                .eventId(event.eventId())
                .triggeredAt(event.reachedAt())
                .status(status)
                .responseCode(responseCode)
                .attemptCount(attemptCount)
                .lastError(lastError)
                .createdAt(Instant.now(clock))
                .build();
        try {
            deliveryRepository.save(delivery);
        } catch (DataIntegrityViolationException _) {
            log.debug("Concurrent duplicate delivery for eventId={}", event.eventId());
        }
    }

    private Counter counter(String status) {
        return Counter.builder(METRIC_NAME).tag(TAG_STATUS, status).register(meterRegistry);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
