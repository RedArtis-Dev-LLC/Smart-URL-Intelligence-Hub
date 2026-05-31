package com.smarturl.hub.webhook.amqp;

import com.smarturl.hub.webhook.amqp.event.ThresholdReachedEvent;
import com.smarturl.hub.webhook.domain.DeliveryStatus;
import com.smarturl.hub.webhook.domain.WebhookDelivery;
import com.smarturl.hub.webhook.domain.WebhookDeliveryRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThresholdDlqConsumer {

    private final WebhookDeliveryRepository deliveryRepository;
    private final Clock clock;

    @RabbitListener(queues = "${webhook.amqp.threshold-dlq}")
    public void onDeadLetter(ThresholdReachedEvent event) {
        log.error("Dead-lettered ThresholdReachedEvent [eventId={}, configId={}]",
                event.eventId(), event.webhookConfigId());

        if (!deliveryRepository.existsByEventId(event.eventId())) {
            var delivery = WebhookDelivery.builder()
                    .id(UUID.randomUUID())
                    .configId(event.webhookConfigId())
                    .eventId(event.eventId())
                    .triggeredAt(event.reachedAt())
                    .status(DeliveryStatus.DEAD)
                    .attemptCount(0)
                    .lastError("Message dead-lettered")
                    .createdAt(Instant.now(clock))
                    .build();
            deliveryRepository.save(delivery);
        }
    }
}
