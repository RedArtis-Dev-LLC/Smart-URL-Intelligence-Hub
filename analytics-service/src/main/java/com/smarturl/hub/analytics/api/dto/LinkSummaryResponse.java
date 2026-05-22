package com.smarturl.hub.analytics.api.dto;

import java.util.List;
import java.util.UUID;

public record LinkSummaryResponse(
        UUID linkId,
        long totalClicks,
        long uniqueClicksToday,
        List<CountryBreakdown> topCountries) {

    public record CountryBreakdown(String country, long clicks) {
    }
}
