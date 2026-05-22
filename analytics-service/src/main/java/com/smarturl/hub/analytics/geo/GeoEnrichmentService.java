package com.smarturl.hub.analytics.geo;

import com.smarturl.hub.analytics.feign.geo.GeoLookupClient;
import com.smarturl.hub.analytics.feign.geo.GeoLookupResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoEnrichmentService {

    public static final String UNKNOWN = "unknown";
    private static final String STATUS_SUCCESS = "success";

    private final GeoLookupClient client;

    public GeoInfo lookup(String ip) {
        if (ip == null || ip.isBlank()) {
            return GeoInfo.unknown();
        }
        try {
            GeoLookupResponse response = client.lookup(ip);
            if (response == null || !STATUS_SUCCESS.equalsIgnoreCase(response.status())) {
                log.debug("Geo lookup non-success for ip={} response={}", ip, response);
                return GeoInfo.unknown();
            }
            return new GeoInfo(
                    nullSafe(response.country()),
                    nullSafe(response.city()));
        } catch (RuntimeException ex) {
            log.warn("Geo lookup failed for ip={}: {}", ip, ex.getMessage());
            return GeoInfo.unknown();
        }
    }

    private static String nullSafe(String value) {
        return value == null || value.isBlank() ? UNKNOWN : value;
    }
}
