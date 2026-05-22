package com.smarturl.hub.analytics.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.analytics.AbstractIntegrationTest;
import com.smarturl.hub.analytics.api.utils.AnalyticsApiUtils;
import com.smarturl.hub.analytics.domain.ClickEventDocument;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

class GetSummaryIT extends AbstractIntegrationTest {

    @Test
    void summary_emptyLink_returnsZeros() {
        //given
        var linkId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        //when
        var result = AnalyticsApiUtils.OK.summary(linkId, userId, restTemplate);

        //then
        assertThat(result.linkId()).isEqualTo(linkId);
        assertThat(result.totalClicks()).isZero();
        assertThat(result.uniqueClicksToday()).isZero();
        assertThat(result.topCountries()).isEmpty();
    }

    @Test
    void summary_withMongoDocs_returnsAggregates() {
        //given
        var linkId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        insertClick(linkId, "Germany", LocalDate.now(ZoneOffset.UTC));
        insertClick(linkId, "Germany", LocalDate.now(ZoneOffset.UTC));
        insertClick(linkId, "USA", LocalDate.now(ZoneOffset.UTC));

        //when
        var result = AnalyticsApiUtils.OK.summary(linkId, userId, restTemplate);

        //then
        assertThat(result.totalClicks()).isEqualTo(3);
        assertThat(result.topCountries())
                .extracting(b -> b.country())
                .containsExactly("Germany", "USA");
        assertThat(result.topCountries())
                .extracting(b -> b.clicks())
                .containsExactly(2L, 1L);
    }

    @Test
    void summary_missingAuthHeaders_returns401() {
        //given
        var linkId = UUID.randomUUID();

        //when
        var response = AnalyticsApiUtils.Error.summary(linkId, new HttpHeaders(), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void insertClick(UUID linkId, String country, LocalDate date) {
        clickEventRepository.save(ClickEventDocument.builder()
                .eventId(UUID.randomUUID())
                .linkId(linkId)
                .userId(UUID.randomUUID())
                .shortCode("abc123")
                .timestamp(date.atTime(12, 0).toInstant(ZoneOffset.UTC))
                .ip("1.2.3.4")
                .country(country)
                .city("X")
                .deviceType("desktop")
                .os("Linux")
                .browser("Firefox")
                .referrer(null)
                .build());
    }

    @SuppressWarnings("unused")
    private static Instant atNoon(LocalDate date) {
        return date.atTime(12, 0).toInstant(ZoneOffset.UTC);
    }
}
