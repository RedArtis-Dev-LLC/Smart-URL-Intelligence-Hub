package com.smarturl.hub.analytics.amqp;

import com.smarturl.hub.analytics.amqp.event.ClickEventMessage;
import com.smarturl.hub.analytics.service.ClickEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClickEventConsumer {

    private final ClickEventProcessor processor;

    @RabbitListener(queues = "${analytics.amqp.click-events-queue}")
    public void onClickEvent(ClickEventMessage event) {
        log.debug("Received click event eventId={} linkId={}", event.eventId(), event.linkId());
        processor.process(event);
    }
}
