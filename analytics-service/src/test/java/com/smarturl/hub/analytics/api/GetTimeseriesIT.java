package com.smarturl.hub.analytics.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.analytics.AbstractIntegrationTest;
import com.smarturl.hub.analytics.api.dto.LinkTimeseriesResponse;
import com.smarturl.hub.analytics.api.utils.AnalyticsApiUtils;
import com.smarturl.hub.analytics.domain.ClickEventDocument;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GetTimeseriesIT extends AbstractIntegrationTest {

    @Test
    void timeseries_emptyRange_returnsZeroBuckets() {
        //given
        var linkId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var from = LocalDate.of(2026, 5, 1);
        var to = LocalDate.of(2026, 5, 3);

        //when
        var result = AnalyticsApiUtils.OK.timeseries(linkId, userId, from, to, restTemplate);

        //then
        assertThat(result.linkId()).isEqualTo(linkId);
        assertThat(result.from()).isEqualTo(from);
        assertThat(result.to()).isEqualTo(to);
        assertThat(result.points()).hasSize(3);
        assertThat(result.points()).extracting(LinkTimeseriesResponse.Bucket::clicks).containsOnly(0L);
    }

    @Test
    void timeseries_withClicks_groupsPerDay() {
        //given
        var linkId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        insertClick(linkId, LocalDate.of(2026, 5, 1));
        insertClick(linkId, LocalDate.of(2026, 5, 1));
        insertClick(linkId, LocalDate.of(2026, 5, 3));

        //when
        var result = AnalyticsApiUtils.OK.timeseries(linkId, userId,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3), restTemplate);

        //then
        assertThat(result.points()).hasSize(3);
        assertThat(result.points().get(0).clicks()).isEqualTo(2L);
        assertThat(result.points().get(1).clicks()).isZero();
        assertThat(result.points().get(2).clicks()).isEqualTo(1L);
    }

    @Test
    void timeseries_fromAfterTo_returns400() {
        //given
        var linkId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        //when
        var response = AnalyticsApiUtils.Error.timeseries(linkId, userId,
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 1), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private void insertClick(UUID linkId, LocalDate date) {
        clickEventRepository.save(ClickEventDocument.builder()
                .eventId(UUID.randomUUID())
                .linkId(linkId)
                .userId(UUID.randomUUID())
                .shortCode("abc")
                .timestamp(date.atTime(12, 0).toInstant(ZoneOffset.UTC))
                .ip("1.2.3.4")
                .country("X")
                .city("X")
                .deviceType("desktop")
                .os("Linux")
                .browser("Firefox")
                .referrer(null)
                .build());
    }
}
