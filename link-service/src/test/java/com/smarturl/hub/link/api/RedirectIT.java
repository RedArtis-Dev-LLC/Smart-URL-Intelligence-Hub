package com.smarturl.hub.link.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.link.AbstractIntegrationTest;
import com.smarturl.hub.link.api.dto.CreateLinkRequest;
import com.smarturl.hub.link.api.utils.LinkApiUtils;
import com.smarturl.hub.link.domain.Link;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

class RedirectIT extends AbstractIntegrationTest {

    @Autowired
    MeterRegistry meterRegistry;

    @Test
    void redirect_happyPath_returns302WithLocationAndNoStore() {
        //given
        var created = LinkApiUtils.OK.create(
                new CreateLinkRequest("https://example.com/destination", null, null, null), userId, restTemplate);

        //when
        ResponseEntity<Void> response = restTemplate.exchange(
                RequestEntity.method(HttpMethod.GET, "/" + created.shortCode()).build(), Void.class);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("https://example.com/destination");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    }

    @Test
    void redirect_incrementsClickCountAndWritesOutboxEvent() {
        //given
        var created = LinkApiUtils.OK.create(
                new CreateLinkRequest("https://example.com", null, null, null), userId, restTemplate);

        //when
        getRedirect(created.shortCode());

        //then
        Link stored = linkRepository.findByShortCode(created.shortCode()).orElseThrow();
        assertThat(stored.getClickCount()).isEqualTo(1L);
        assertThat(outboxRepository.findAll())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getAggregateId()).isEqualTo(stored.getId());
                    assertThat(event.getEventType()).isEqualTo("CLICK_EVENT");
                    assertThat(event.isPublished()).isFalse();
                    assertThat(event.getPayload()).contains(created.shortCode());
                });
    }

    @Test
    void redirect_incrementsRedirectCounter() {
        //given
        var created = LinkApiUtils.OK.create(
                new CreateLinkRequest("https://example.com", null, null, null), userId, restTemplate);

        //when
        getRedirect(created.shortCode());
        getRedirect(created.shortCode());

        //then
        double count = Search.in(meterRegistry).name("link.redirects.total").counter().count();
        assertThat(count).isGreaterThanOrEqualTo(2.0);
    }

    @Test
    void redirect_unknownShortCode_returns404() {
        //when
        ResponseEntity<ProblemDetail> response = restTemplate.exchange(
                RequestEntity.method(HttpMethod.GET, "/nonexist").build(), ProblemDetail.class);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Not Found");
    }

    @Test
    void redirect_inactiveLink_returns410() {
        //given
        var created = LinkApiUtils.OK.create(
                new CreateLinkRequest("https://example.com", null, null, null), userId, restTemplate);
        LinkApiUtils.OK.delete(created.id(), userId, restTemplate);

        //when
        ResponseEntity<ProblemDetail> response = restTemplate.exchange(
                RequestEntity.method(HttpMethod.GET, "/" + created.shortCode()).build(), ProblemDetail.class);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).contains("inactive");
    }

    @Test
    void redirect_expiredLink_returns410() {
        //given
        var expired = new CreateLinkRequest(
                "https://example.com", null, Instant.now().minus(1, ChronoUnit.HOURS), null);
        var created = LinkApiUtils.OK.create(expired, userId, restTemplate);

        //when
        ResponseEntity<ProblemDetail> response = restTemplate.exchange(
                RequestEntity.method(HttpMethod.GET, "/" + created.shortCode()).build(), ProblemDetail.class);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).contains("expired");
    }

    @Test
    void redirect_maxClicksReached_returns410OnNextHit() {
        //given
        var capped = new CreateLinkRequest("https://example.com", null, null, 1L);
        var created = LinkApiUtils.OK.create(capped, userId, restTemplate);

        //when
        ResponseEntity<Void> first = getRedirect(created.shortCode());
        ResponseEntity<ProblemDetail> second = restTemplate.exchange(
                RequestEntity.method(HttpMethod.GET, "/" + created.shortCode()).build(), ProblemDetail.class);

        //then
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().getDetail()).contains("maximum clicks");
    }

    @Test
    void redirect_isPublic_doesNotRequireAuthHeaders() {
        //given
        var created = LinkApiUtils.OK.create(
                new CreateLinkRequest("https://example.com", null, null, null), userId, restTemplate);

        //when - no X-User-* headers set
        ResponseEntity<Void> response = restTemplate.exchange(
                RequestEntity.method(HttpMethod.GET, "/" + created.shortCode()).build(), Void.class);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
    }

    private ResponseEntity<Void> getRedirect(String shortCode) {
        return restTemplate.exchange(
                RequestEntity.method(HttpMethod.GET, "/" + shortCode)
                        .header(HttpHeaders.USER_AGENT, "JUnit-Test")
                        .header(HttpHeaders.REFERER, "https://test.referrer/path")
                        .build(),
                Void.class);
    }
}
