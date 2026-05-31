package com.smarturl.hub.webhook.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.webhook.AbstractIntegrationTest;
import com.smarturl.hub.webhook.api.utils.WebhookApiUtils;
import com.smarturl.hub.webhook.domain.WebhookConfig;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InternalWebhookIT extends AbstractIntegrationTest {

    @Test
    void getThresholds_returnsActiveConfigs() {
        var linkId = UUID.randomUUID();
        configRepository.save(WebhookConfig.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID()).linkId(linkId)
                .targetUrl("https://example.com/hook").threshold(100)
                .active(true).createdAt(Instant.now()).build());
        configRepository.save(WebhookConfig.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID()).linkId(linkId)
                .targetUrl("https://example.com/hook2").threshold(500)
                .active(false).createdAt(Instant.now()).build());

        var thresholds = WebhookApiUtils.OK.thresholds(linkId, restTemplate);

        assertThat(thresholds).hasSize(1);
        assertThat(thresholds[0].threshold()).isEqualTo(100);
    }

    @Test
    void getThresholds_noAuth_stillWorks() {
        var thresholds = WebhookApiUtils.OK.thresholds(UUID.randomUUID(), restTemplate);
        assertThat(thresholds).isEmpty();
    }
}
