package com.smarturl.hub.analytics.amqp;

import com.smarturl.hub.analytics.amqp.event.ThresholdReachedEvent;
import com.smarturl.hub.analytics.config.AnalyticsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThresholdReachedPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final AnalyticsProperties properties;

    public void publish(ThresholdReachedEvent event) {
        rabbitTemplate.convertAndSend(
                properties.amqp().thresholdExchange(),
                properties.amqp().thresholdRoutingKey(),
                event);
        log.info("Published ThresholdReachedEvent [linkId={}, configId={}, threshold={}, count={}]",
                event.linkId(), event.webhookConfigId(), event.threshold(), event.currentCount());
    }
}
