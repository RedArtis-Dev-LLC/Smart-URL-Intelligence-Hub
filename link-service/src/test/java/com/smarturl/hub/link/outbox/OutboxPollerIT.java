package com.smarturl.hub.link.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.smarturl.hub.link.AbstractIntegrationTest;
import com.smarturl.hub.link.TestContainersConfig;
import com.smarturl.hub.link.api.dto.CreateLinkRequest;
import com.smarturl.hub.link.api.utils.LinkApiUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;

class OutboxPollerIT extends AbstractIntegrationTest {

    @Autowired
    MeterRegistry meterRegistry;

    @BeforeEach
    void drainQueue() {
        rabbitAdmin.purgeQueue(TestContainersConfig.TEST_QUEUE, false);
    }

    @Test
    void poller_publishesUnpublishedEvents_andMarksThemPublished() {
        //given a redirect that writes one outbox event
        var link = LinkApiUtils.OK.create(
                new CreateLinkRequest("https://example.com", null, null, null), userId, restTemplate);
        restTemplate.exchange(
                RequestEntity.method(HttpMethod.GET, "/" + link.shortCode()).build(), Void.class);

        //then the poller (running every 500ms in test profile) publishes and marks it
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(outboxRepository.findAll()).singleElement()
                        .satisfies(event -> assertThat(event.isPublished()).isTrue()));

        Message message = rabbitTemplate.receive(TestContainersConfig.TEST_QUEUE, 2000L);
        assertThat(message).isNotNull();
        String body = new String(message.getBody());
        assertThat(body).contains(link.shortCode());
        Object eventType = message.getMessageProperties().getHeader("eventType");
        assertThat(eventType).isEqualTo("CLICK_EVENT");
    }

    @Test
    void gauge_reflectsUnpublishedCount() {
        //given a fresh state
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(Double.valueOf(unpublishedGaugeValue())).isZero());

        //when many redirects happen quickly
        var link = LinkApiUtils.OK.create(
                new CreateLinkRequest("https://example.com", null, null, null), userId, restTemplate);
        for (int i = 0; i < 3; i++) {
            restTemplate.exchange(
                    RequestEntity.method(HttpMethod.GET, "/" + link.shortCode()).build(), Void.class);
        }

        //then the poller drains them; gauge returns to 0
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(Double.valueOf(unpublishedGaugeValue())).isZero());
    }

    private double unpublishedGaugeValue() {
        return Search.in(meterRegistry).name("outbox.pending.events").gauge().value();
    }
}
