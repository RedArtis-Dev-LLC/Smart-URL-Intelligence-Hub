package com.smarturl.hub.webhook.amqp;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.smarturl.hub.webhook.AbstractIntegrationTest;
import com.smarturl.hub.webhook.amqp.event.ThresholdReachedEvent;
import com.smarturl.hub.webhook.domain.DeliveryStatus;
import com.smarturl.hub.webhook.domain.WebhookConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WebhookDeliveryIT extends AbstractIntegrationTest {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("webhook-delivery").reset();
    }

    @Test
    void deliver_happyPath_persistsSuccessRecord() {
        var configId = UUID.randomUUID();
        var linkId = UUID.randomUUID();
        createConfig(configId, linkId, "/webhook/target");

        wireMockServer.stubFor(post(urlEqualTo("/webhook/target"))
                .willReturn(aResponse().withStatus(200)));

        var event = newEvent(linkId, configId);
        publishThresholdEvent(event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var deliveries = deliveryRepository.findAll();
            assertThat(deliveries).hasSize(1);
            assertThat(deliveries.getFirst().getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
            assertThat(deliveries.getFirst().getResponseCode()).isEqualTo(200);
        });

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/webhook/target")));
    }

    @Test
    void deliver_targetReturns500_retriesAndPersistsFailedRecord() {
        var configId = UUID.randomUUID();
        var linkId = UUID.randomUUID();
        createConfig(configId, linkId, "/webhook/failing");

        wireMockServer.stubFor(post(urlEqualTo("/webhook/failing"))
                .willReturn(aResponse().withStatus(500)));

        var event = newEvent(linkId, configId);
        publishThresholdEvent(event);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var deliveries = deliveryRepository.findAll();
            assertThat(deliveries).hasSize(1);
            assertThat(deliveries.getFirst().getStatus()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(deliveries.getFirst().getAttemptCount()).isEqualTo(3);
        });
    }

    @Test
    void deliver_duplicateEvent_skipsSecondDelivery() {
        var configId = UUID.randomUUID();
        var linkId = UUID.randomUUID();
        createConfig(configId, linkId, "/webhook/dedup");

        wireMockServer.stubFor(post(urlEqualTo("/webhook/dedup"))
                .willReturn(aResponse().withStatus(200)));

        var event = newEvent(linkId, configId);
        publishThresholdEvent(event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(deliveryRepository.findAll()).hasSize(1));

        // publish same event again
        publishThresholdEvent(event);
        // wait a bit and verify no second delivery
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(deliveryRepository.findAll()).hasSize(1));
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
                UUID.randomUUID(),
                linkId,
                "promo",
                "https://example.com/original",
                configId,
                100,
                100,
                Instant.now());
    }

    private void publishThresholdEvent(ThresholdReachedEvent event) {
        rabbitTemplate.convertAndSend(
                webhookProperties.amqp().thresholdExchange(),
                webhookProperties.amqp().thresholdRoutingKey(),
                event);
    }
}
