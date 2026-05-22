package com.smarturl.hub.analytics.geo;

public record GeoInfo(String country, String city) {

    public static GeoInfo unknown() {
        return new GeoInfo(GeoEnrichmentService.UNKNOWN, GeoEnrichmentService.UNKNOWN);
    }
}
