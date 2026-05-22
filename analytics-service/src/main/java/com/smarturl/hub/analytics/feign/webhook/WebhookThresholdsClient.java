package com.smarturl.hub.analytics.feign.webhook;

import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "webhook-service", url = "${analytics.webhook-service.base-url}")
public interface WebhookThresholdsClient {

    @GetMapping("/internal/webhooks/thresholds")
    List<WebhookThreshold> findByLinkId(@RequestParam("linkId") UUID linkId);
}
