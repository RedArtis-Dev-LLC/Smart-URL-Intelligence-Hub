package com.smarturl.hub.webhook.amqp;

import com.smarturl.hub.webhook.amqp.event.ThresholdReachedEvent;
import com.smarturl.hub.webhook.service.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThresholdEventConsumer {

    private final WebhookDeliveryService deliveryService;

    @RabbitListener(queues = "${webhook.amqp.threshold-queue}")
    public void onThresholdReached(ThresholdReachedEvent event) {
        log.info("Received ThresholdReachedEvent [eventId={}, linkId={}, threshold={}]",
                event.eventId(), event.linkId(), event.threshold());
        deliveryService.deliver(event);
    }
}
