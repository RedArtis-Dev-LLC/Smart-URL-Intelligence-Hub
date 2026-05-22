package com.smarturl.hub.analytics.domain;

import java.time.LocalDate;

public record DailyClickCount(LocalDate date, long clicks) {
}
