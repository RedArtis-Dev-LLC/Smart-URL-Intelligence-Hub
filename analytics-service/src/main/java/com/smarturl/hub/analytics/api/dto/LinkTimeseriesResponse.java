package com.smarturl.hub.analytics.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LinkTimeseriesResponse(
        UUID linkId,
        LocalDate from,
        LocalDate to,
        List<Bucket> points) {

    public record Bucket(LocalDate date, long clicks) {
    }
}
