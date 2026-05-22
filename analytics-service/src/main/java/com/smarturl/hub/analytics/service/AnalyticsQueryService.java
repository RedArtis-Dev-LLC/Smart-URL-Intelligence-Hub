package com.smarturl.hub.analytics.service;

import com.smarturl.hub.analytics.api.dto.LinkSummaryResponse;
import com.smarturl.hub.analytics.api.dto.LinkSummaryResponse.CountryBreakdown;
import com.smarturl.hub.analytics.api.dto.LinkTimeseriesResponse;
import com.smarturl.hub.analytics.api.dto.LinkTimeseriesResponse.Bucket;
import com.smarturl.hub.analytics.domain.ClickEventRepository;
import com.smarturl.hub.analytics.domain.DailyClickCount;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsQueryService {

    private static final int TOP_COUNTRIES = 5;

    private final ClickEventRepository repository;
    private final ClickCounterService clickCounterService;
    private final Clock clock;

    public LinkSummaryResponse summary(UUID linkId) {
        long total = clickCounterService.getTotal(linkId);
        if (total == 0L) {
            total = repository.countByLinkId(linkId);
        }
        long uniqueToday = clickCounterService.countUnique(linkId, today());
        List<CountryBreakdown> top = repository.aggregateCountryCounts(linkId).stream()
                .limit(TOP_COUNTRIES)
                .map(c -> new CountryBreakdown(c.country(), c.clicks()))
                .toList();
        return new LinkSummaryResponse(linkId, total, uniqueToday, top);
    }

    public LinkTimeseriesResponse timeseries(UUID linkId, LocalDate from, LocalDate to) {
        var inclusiveTo = to.plusDays(1);
        var fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        var toInstant = inclusiveTo.atStartOfDay(ZoneOffset.UTC).toInstant();

        List<DailyClickCount> raw = repository.aggregateDailyCounts(linkId, fromInstant, toInstant);
        Map<LocalDate, Long> byDay = new HashMap<>();
        for (DailyClickCount row : raw) {
            byDay.put(row.date(), row.clicks());
        }

        List<Bucket> buckets = from.datesUntil(inclusiveTo)
                .map(date -> new Bucket(date, byDay.getOrDefault(date, 0L)))
                .toList();
        return new LinkTimeseriesResponse(linkId, from, to, buckets);
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
