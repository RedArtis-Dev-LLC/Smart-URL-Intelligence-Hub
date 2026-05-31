package com.smarturl.hub.webhook.amqp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.smarturl.hub.webhook.AbstractIntegrationTest;
import com.smarturl.hub.webhook.amqp.event.ThresholdReachedEvent;
import com.smarturl.hub.webhook.domain.DeliveryStatus;
import com.smarturl.hub.webhook.domain.WebhookConfig;
import com.smarturl.hub.webhook.service.WebhookDeliveryService;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class ThresholdDlqFullFlowIT extends AbstractIntegrationTest {

    @MockitoSpyBean
    private WebhookDeliveryService deliveryService;

    /**
     * Full flow: event published to main exchange → main consumer throws (simulated) →
     * message dead-lettered → DLQ consumer saves a DEAD delivery record.
     */
    @Test
    void fullFlow_mainConsumerFails_dlqConsumerSavesDeadDelivery() {
        var configId = UUID.randomUUID();
        saveConfig(configId);
        var event = newEvent(configId);

        // Force the main consumer to throw so the message is dead-lettered
        doThrow(new RuntimeException("simulated processing failure"))
                .when(deliveryService).deliver(any());

        publishThresholdEvent(event);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var deliveries = deliveryRepository.findAll();
            assertThat(deliveries).hasSize(1);
            var delivery = deliveries.getFirst();
            assertThat(delivery.getEventId()).isEqualTo(event.eventId());
            assertThat(delivery.getConfigId()).isEqualTo(configId);
            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DEAD);
            assertThat(delivery.getLastError()).isEqualTo("Message dead-lettered");
        });
    }

    private void saveConfig(UUID configId) {
        configRepository.save(WebhookConfig.builder()
                .id(configId)
                .userId(UUID.randomUUID())
                .linkId(UUID.randomUUID())
                .targetUrl("https://hooks.example.com/test")
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

    private void publishThresholdEvent(ThresholdReachedEvent event) {
        rabbitTemplate.convertAndSend(
                webhookProperties.amqp().thresholdExchange(),
                webhookProperties.amqp().thresholdRoutingKey(),
                event);
    }
}
