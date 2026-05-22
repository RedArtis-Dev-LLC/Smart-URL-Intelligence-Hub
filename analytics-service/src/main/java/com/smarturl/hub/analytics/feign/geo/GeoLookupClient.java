package com.smarturl.hub.analytics.feign.geo;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "geo-lookup", url = "${analytics.geo.base-url}")
public interface GeoLookupClient {

    @GetMapping(value = "/json/{ip}", produces = "application/json")
    GeoLookupResponse lookup(@PathVariable("ip") String ip);
}
