package com.smarturl.hub.webhook.feign;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "link-service", url = "${webhook.link-service.base-url}")
public interface LinkServiceClient {

    @GetMapping("/links/{linkId}")
    LinkResponse getLink(@PathVariable UUID linkId, @RequestHeader("X-User-Id") String userId);
}
