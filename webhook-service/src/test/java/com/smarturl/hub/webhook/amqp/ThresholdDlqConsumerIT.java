package com.smarturl.hub.webhook.amqp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.smarturl.hub.webhook.AbstractIntegrationTest;
import com.smarturl.hub.webhook.amqp.event.ThresholdReachedEvent;
import com.smarturl.hub.webhook.domain.DeliveryStatus;
import com.smarturl.hub.webhook.domain.WebhookConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ThresholdDlqConsumerIT extends AbstractIntegrationTest {

    @Test
    void onDeadLetter_newEvent_savesDeadDelivery() {
        var configId = UUID.randomUUID();
        saveConfig(configId);
        var event = newEvent(configId);

        publishToDlq(event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var deliveries = deliveryRepository.findAll();
            assertThat(deliveries).hasSize(1);
            var delivery = deliveries.getFirst();
            assertThat(delivery.getEventId()).isEqualTo(event.eventId());
            assertThat(delivery.getConfigId()).isEqualTo(configId);
            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DEAD);
            assertThat(delivery.getAttemptCount()).isZero();
            assertThat(delivery.getLastError()).isEqualTo("Message dead-lettered");
        });
    }

    @Test
    void onDeadLetter_duplicateEvent_skipsSecondSave() {
        var configId = UUID.randomUUID();
        saveConfig(configId);
        var event = newEvent(configId);

        publishToDlq(event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(deliveryRepository.findAll()).hasSize(1));

        publishToDlq(event);

        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(deliveryRepository.findAll()).hasSize(1));
    }

    private void saveConfig(UUID configId) {
        configRepository.save(WebhookConfig.builder()
                .id(configId)
                .userId(UUID.randomUUID())
                .linkId(UUID.randomUUID())
                .targetUrl("https://hooks.example.com/dlq-test")
                .threshold(100)
                .active(true)
                .createdAt(Instant.now())
                .build());
    }

    private ThresholdReachedEvent newEvent(UUID configId) {
        return new ThresholdReachedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "promo",
                "https://example.com/original",
                configId,
                100,
                100,
                Instant.now());
    }

    private void publishToDlq(ThresholdReachedEvent event) {
        rabbitTemplate.convertAndSend(webhookProperties.amqp().thresholdDlq(), event);
    }
}
