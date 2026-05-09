package com.smarturl.hub.link.outbox;

import com.smarturl.hub.link.amqp.ClickEventPublisher;
import com.smarturl.hub.link.config.LinkProperties;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxRepository repository;
    private final ClickEventPublisher publisher;
    private final LinkProperties properties;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    @PostConstruct
    void registerGauge() {
        Gauge.builder("outbox.pending.events", repository, OutboxRepository::countByPublishedFalse)
                .description("Unpublished outbox events waiting to be dispatched")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${link.outbox.poll-interval-ms}")
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> batch = repository.findUnpublishedForUpdate(properties.outbox().batchSize());
        if (batch.isEmpty()) {
            return;
        }
        int published = 0;
        for (OutboxEvent event : batch) {
            try {
                publisher.publish(event);
                event.setPublished(true);
                published++;
            } catch (RuntimeException ex) {
                log.error("Failed to publish outbox event [eventId={}]; will retry on next poll",
                        event.getId(), ex);
            }
        }
        log.debug("Outbox poller published {}/{} events", published, batch.size());
    }

    @Scheduled(cron = "${link.outbox.cleanup-cron}")
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now(clock).minus(properties.outbox().retentionPeriod());
        int deleted = repository.deletePublishedBefore(cutoff);
        if (deleted > 0) {
            log.info("Outbox cleanup removed {} published events older than {}", deleted, cutoff);
        }
    }
}
