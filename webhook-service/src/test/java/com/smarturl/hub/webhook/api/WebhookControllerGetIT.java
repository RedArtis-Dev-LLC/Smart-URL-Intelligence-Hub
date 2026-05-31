package com.smarturl.hub.webhook.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.webhook.AbstractIntegrationTest;
import com.smarturl.hub.webhook.api.dto.CreateWebhookRequest;
import com.smarturl.hub.webhook.api.utils.WebhookApiUtils;
import com.smarturl.hub.webhook.mocks.LinkServiceMocks;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class WebhookControllerGetIT extends AbstractIntegrationTest {

    @Test
    void get_happyPath_returnsConfig() {
        var userId = UUID.randomUUID();
        var linkId = UUID.randomUUID();
        LinkServiceMocks.mockGetLink_200(wireMockServer, linkId, userId);

        var created = WebhookApiUtils.OK.create(
                new CreateWebhookRequest(linkId, "https://hooks.example.com/cb", 50), userId, restTemplate);

        var fetched = WebhookApiUtils.OK.get(created.id(), userId, restTemplate);

        assertThat(fetched.id()).isEqualTo(created.id());
        assertThat(fetched.linkId()).isEqualTo(linkId);
        assertThat(fetched.threshold()).isEqualTo(50);
    }

    @Test
    void get_notFound_returns404() {
        var response = WebhookApiUtils.Error.get(UUID.randomUUID(), UUID.randomUUID(), restTemplate);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void get_otherUsersConfig_returns403() {
        var owner = UUID.randomUUID();
        var linkId = UUID.randomUUID();
        LinkServiceMocks.mockGetLink_200(wireMockServer, linkId, owner);

        var created = WebhookApiUtils.OK.create(
                new CreateWebhookRequest(linkId, "https://hooks.example.com/cb", 100), owner, restTemplate);

        var response = WebhookApiUtils.Error.get(created.id(), UUID.randomUUID(), restTemplate);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void get_unauthenticated_returns401() {
        var response = WebhookApiUtils.Error.getNoAuth(UUID.randomUUID(), restTemplate);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
