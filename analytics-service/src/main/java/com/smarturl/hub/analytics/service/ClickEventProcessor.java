package com.smarturl.hub.analytics.service;

import com.smarturl.hub.analytics.amqp.event.ClickEventMessage;
import com.smarturl.hub.analytics.domain.ClickEventDocument;
import com.smarturl.hub.analytics.domain.ClickEventRepository;
import com.smarturl.hub.analytics.geo.GeoEnrichmentService;
import com.smarturl.hub.analytics.geo.GeoInfo;
import com.smarturl.hub.analytics.ua.UserAgentInfo;
import com.smarturl.hub.analytics.ua.UserAgentParser;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClickEventProcessor {

    static final String COUNTER_NAME = "click.events.processed.total";
    static final String TAG_STATUS = "status";
    static final String STATUS_SUCCESS = "success";
    static final String STATUS_DUPLICATE = "duplicate";

    private final ClickEventRepository repository;
    private final GeoEnrichmentService geoEnrichmentService;
    private final UserAgentParser userAgentParser;
    private final ClickCounterService clickCounterService;
    private final ThresholdDetector thresholdDetector;
    private final MeterRegistry meterRegistry;

    public void process(ClickEventMessage event) {
        if (repository.existsByEventId(event.eventId())) {
            log.debug("Duplicate click event eventId={} linkId={}", event.eventId(), event.linkId());
            counter(STATUS_DUPLICATE).increment();
            return;
        }

        GeoInfo geo = geoEnrichmentService.lookup(event.ip());
        UserAgentInfo ua = userAgentParser.parse(event.userAgent());

        var document = ClickEventDocument.builder()
                .eventId(event.eventId())
                .linkId(event.linkId())
                .userId(event.userId())
                .shortCode(event.shortCode())
                .timestamp(event.timestamp())
                .ip(event.ip())
                .country(geo.country())
                .city(geo.city())
                .deviceType(ua.deviceType())
                .os(ua.os())
                .browser(ua.browser())
                .referrer(event.referrer())
                .build();

        try {
            repository.insert(document);
        } catch (DuplicateKeyException _) {
            log.debug("Concurrent duplicate eventId={} suppressed", event.eventId());
            counter(STATUS_DUPLICATE).increment();
            return;
        }

        long total = clickCounterService.incrementTotal(event.linkId());
        clickCounterService.addUniqueVisitor(event.linkId(), event.ip());
        thresholdDetector.check(event.linkId(), event.shortCode(), event.originalUrl(), total);
        counter(STATUS_SUCCESS).increment();
    }

    private Counter counter(String status) {
        return Counter.builder(COUNTER_NAME)
                .tag(TAG_STATUS, status)
                .register(meterRegistry);
    }
}
