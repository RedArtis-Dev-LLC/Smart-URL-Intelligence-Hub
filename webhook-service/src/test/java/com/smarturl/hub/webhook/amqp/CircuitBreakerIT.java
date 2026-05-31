package com.smarturl.hub.webhook.amqp;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.smarturl.hub.webhook.AbstractIntegrationTest;
import com.smarturl.hub.webhook.amqp.event.ThresholdReachedEvent;
import com.smarturl.hub.webhook.domain.DeliveryStatus;
import com.smarturl.hub.webhook.domain.WebhookConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CircuitBreakerIT extends AbstractIntegrationTest {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("webhook-delivery").reset();
    }

    @Test
    void circuitBreaker_opensAfterFailures_subsequentDeliveryFailsFast() {
        var linkId = UUID.randomUUID();
        var configId = UUID.randomUUID();
        createConfig(configId, linkId, "/webhook/cb-test");

        // stub always returns 500 to cause failures
        wireMockServer.stubFor(post(urlEqualTo("/webhook/cb-test"))
                .willReturn(aResponse().withStatus(500)));

        // first event: 3 retry attempts all fail → CB records 3 failures → CB opens
        var event1 = newEvent(linkId, configId);
        publishThresholdEvent(event1);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var deliveries = deliveryRepository.findAll();
            assertThat(deliveries).hasSize(1);
            assertThat(deliveries.getFirst().getStatus()).isEqualTo(DeliveryStatus.FAILED);
        });

        // verify CB is now open
        var cb = circuitBreakerRegistry.circuitBreaker("webhook-delivery");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // second event: should fail fast with circuit_open
        var event2 = newEvent(linkId, configId);
        publishThresholdEvent(event2);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var deliveries = deliveryRepository.findAll();
            assertThat(deliveries).hasSize(2);
            var second = deliveries.stream()
                    .filter(d -> d.getEventId().equals(event2.eventId()))
                    .findFirst().orElseThrow();
            assertThat(second.getStatus()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(second.getLastError()).isEqualTo("Circuit breaker open");
        });
    }

    private WebhookConfig createConfig(UUID configId, UUID linkId, String path) {
        return configRepository.save(WebhookConfig.builder()
                .id(configId)
                .userId(UUID.randomUUID())
                .linkId(linkId)
                .targetUrl(wireMockServer.baseUrl() + path)
                .threshold(100)
                .active(true)
                .createdAt(Instant.now())
                .build());
    }

    private ThresholdReachedEvent newEvent(UUID linkId, UUID configId) {
        return new ThresholdReachedEvent(
                UUID.randomUUID(), linkId, "promo", "https://example.com/original",
                configId, 100, 100, Instant.now());
    }

    private void publishThresholdEvent(ThresholdReachedEvent event) {
        rabbitTemplate.convertAndSend(
                webhookProperties.amqp().thresholdExchange(),
                webhookProperties.amqp().thresholdRoutingKey(),
                event);
    }
}
