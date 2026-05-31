package com.smarturl.hub.webhook.api;

import com.smarturl.hub.webhook.api.dto.WebhookThresholdResponse;
import com.smarturl.hub.webhook.service.WebhookConfigService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/webhooks")
@RequiredArgsConstructor
public class InternalWebhookController {

    private final WebhookConfigService service;

    @GetMapping("/thresholds")
    public List<WebhookThresholdResponse> getThresholds(@RequestParam UUID linkId) {
        return service.getThresholdsByLinkId(linkId);
    }
}
