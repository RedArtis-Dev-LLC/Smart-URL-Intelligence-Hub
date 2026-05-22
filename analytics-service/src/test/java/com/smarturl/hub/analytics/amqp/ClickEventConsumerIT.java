package com.smarturl.hub.analytics.amqp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.smarturl.hub.analytics.AbstractIntegrationTest;
import com.smarturl.hub.analytics.amqp.event.ClickEventMessage;
import com.smarturl.hub.analytics.feign.webhook.WebhookThreshold;
import com.smarturl.hub.analytics.mocks.GeoApiMocks;
import com.smarturl.hub.analytics.mocks.WebhookServiceMocks;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;

class ClickEventConsumerIT extends AbstractIntegrationTest {

    private static final String THRESHOLD_PROBE_QUEUE = "test.threshold.probe.queue";

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    private String probeQueueName;
    private double successBaseline;
    private double duplicateBaseline;

    @BeforeEach
    void declareProbeQueue() {
        var queue = QueueBuilder.durable(THRESHOLD_PROBE_QUEUE).build();
        rabbitAdmin.declareQueue(queue);
        var exchange = new TopicExchange(analyticsProperties.amqp().thresholdExchange(), true, false);
        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange)
                .with(analyticsProperties.amqp().thresholdRoutingKey()));
        probeQueueName = queue.getName();
        rabbitAdmin.purgeQueue(probeQueueName, false);
        successBaseline = successCounterValue();
        duplicateBaseline = duplicateCounterValue();
    }

    @Test
    void consume_happyPath_persistsToMongoAndIncrementsCounters() {
        //given
        GeoApiMocks.mockLookup_200(wireMockServer);
        WebhookServiceMocks.mockThresholds_emptyForAny(wireMockServer);
        var linkId = UUID.randomUUID();
        var event = newClickEvent(linkId);

        //when
        publish(event);

        //then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(clickEventRepository.existsByEventId(event.eventId())).isTrue());

        var stored = clickEventRepository.findAll().getFirst();
        assertThat(stored.getCountry()).isEqualTo("Germany");
        assertThat(stored.getCity()).isEqualTo("Berlin");
        assertThat(stored.getBrowser()).isNotEqualTo("unknown");

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(redis.opsForValue().get("click:total:" + linkId)).isEqualTo("1"));

        assertThat(successCounterValue() - successBaseline).isEqualTo(1.0);
    }

    @Test
    void consume_duplicateEventId_acksAndSkips() {
        //given
        GeoApiMocks.mockLookup_200(wireMockServer);
        WebhookServiceMocks.mockThresholds_emptyForAny(wireMockServer);
        var event = newClickEvent(UUID.randomUUID());
        publish(event);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(clickEventRepository.existsByEventId(event.eventId())).isTrue());

        //when -- publish the same event again
        publish(event);

        //then
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(duplicateCounterValue() - duplicateBaseline).isEqualTo(1.0));
        assertThat(clickEventRepository.count()).isEqualTo(1);
    }

    @Test
    void consume_thresholdHit_publishesThresholdReachedEvent() {
        //given
        var linkId = UUID.randomUUID();
        var configId = UUID.randomUUID();
        GeoApiMocks.mockLookup_200(wireMockServer);
        WebhookServiceMocks.mockThresholds_200(wireMockServer, linkId,
                List.of(new WebhookThreshold(configId, 1L)));

        //when
        publish(newClickEvent(linkId));

        //then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Message raw = rabbitTemplate.receive(probeQueueName, 1000L);
            assertThat(raw).isNotNull();
            String body = new String(raw.getBody());
            assertThat(body)
                    .contains(linkId.toString())
                    .contains(configId.toString())
                    .contains("\"threshold\":1")
                    .contains("\"currentCount\":1");
        });
    }

    @Test
    void consume_thresholdNotMatched_doesNotPublish() {
        //given
        var linkId = UUID.randomUUID();
        var event = newClickEvent(linkId);
        GeoApiMocks.mockLookup_200(wireMockServer);
        WebhookServiceMocks.mockThresholds_200(wireMockServer, linkId,
                List.of(new WebhookThreshold(UUID.randomUUID(), 10L)));

        //when
        publish(event);

        //then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(clickEventRepository.existsByEventId(event.eventId())).isTrue());
        Message probe = rabbitTemplate.receive(probeQueueName, 500L);
        assertThat(probe).isNull();
    }

    @Test
    void consume_geoLookupFails_persistsWithUnknownGeo() {
        //given
        GeoApiMocks.mockLookup_500(wireMockServer);
        WebhookServiceMocks.mockThresholds_emptyForAny(wireMockServer);
        var event = newClickEvent(UUID.randomUUID());

        //when
        publish(event);

        //then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(clickEventRepository.existsByEventId(event.eventId())).isTrue());
        var stored = clickEventRepository.findAll().getFirst();
        assertThat(stored.getCountry()).isEqualTo("unknown");
    }

    private ClickEventMessage newClickEvent(UUID linkId) {
        return new ClickEventMessage(
                UUID.randomUUID(),
                linkId,
                UUID.randomUUID(),
                "abcd123",
                Instant.now(),
                "203.0.113.42",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "https://referrer.example");
    }

    private void publish(ClickEventMessage event) {
        rabbitTemplate.convertAndSend(
                analyticsProperties.amqp().clickEventsExchange(),
                analyticsProperties.amqp().clickEventsRoutingKey(),
                event);
    }

    private double successCounterValue() {
        var counter = Search.in(meterRegistry).name("click.events.processed.total")
                .tag("status", "success").counter();
        return counter == null ? 0.0 : counter.count();
    }

    private double duplicateCounterValue() {
        var counter = Search.in(meterRegistry).name("click.events.processed.total")
                .tag("status", "duplicate").counter();
        return counter == null ? 0.0 : counter.count();
    }
}
