package com.smarturl.hub.analytics.api;

import com.smarturl.hub.analytics.api.dto.LinkSummaryResponse;
import com.smarturl.hub.analytics.api.dto.LinkTimeseriesResponse;
import com.smarturl.hub.analytics.service.AnalyticsQueryService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/analytics/links")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final int MAX_RANGE_DAYS = 366;

    private final AnalyticsQueryService queryService;
    private final Clock clock;

    @GetMapping("/{linkId}/summary")
    public LinkSummaryResponse summary(@PathVariable UUID linkId) {
        return queryService.summary(linkId);
    }

    @GetMapping("/{linkId}/timeseries")
    public LinkTimeseriesResponse timeseries(
            @PathVariable UUID linkId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDate effectiveTo = to != null ? to : today;
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(6);

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must not be after 'to'");
        }
        if (effectiveFrom.plusDays(MAX_RANGE_DAYS).isBefore(effectiveTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date range exceeds maximum of " + MAX_RANGE_DAYS + " days");
        }
        return queryService.timeseries(linkId, effectiveFrom, effectiveTo);
    }
}
