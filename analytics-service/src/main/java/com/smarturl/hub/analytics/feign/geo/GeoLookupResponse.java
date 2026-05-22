package com.smarturl.hub.analytics.feign.geo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoLookupResponse(
        String status,
        String country,
        String city,
        String message) {
}
