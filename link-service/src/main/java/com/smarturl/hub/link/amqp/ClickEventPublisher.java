package com.smarturl.hub.link.amqp;

import com.smarturl.hub.link.config.LinkProperties;
import com.smarturl.hub.link.outbox.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClickEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final LinkProperties properties;

    public void publish(OutboxEvent event) {
        rabbitTemplate.convertAndSend(
                properties.amqp().clickEventsExchange(),
                properties.amqp().clickEventsRoutingKey(),
                event.getPayload(),
                message -> {
                    var props = message.getMessageProperties();
                    props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    props.setHeader("eventId", event.getId().toString());
                    props.setHeader("eventType", event.getEventType());
                    return message;
                });
        log.debug("Published outbox event [eventId={}, type={}]", event.getId(), event.getEventType());
    }
}
