package com.smarturl.hub.analytics.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ClickEventAggregationRepository {

    List<DailyClickCount> aggregateDailyCounts(UUID linkId, Instant from, Instant to);

    List<CountryClickCount> aggregateCountryCounts(UUID linkId);
}
