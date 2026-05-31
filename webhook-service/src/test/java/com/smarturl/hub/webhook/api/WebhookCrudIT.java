package com.smarturl.hub.webhook.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.webhook.AbstractIntegrationTest;
import com.smarturl.hub.webhook.api.dto.CreateWebhookRequest;
import com.smarturl.hub.webhook.api.dto.UpdateWebhookRequest;
import com.smarturl.hub.webhook.api.utils.WebhookApiUtils;
import com.smarturl.hub.webhook.mocks.LinkServiceMocks;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class WebhookCrudIT extends AbstractIntegrationTest {

    @Test
    void create_happyPath_returns201() {
        var userId = UUID.randomUUID();
        var linkId = UUID.randomUUID();
        LinkServiceMocks.mockGetLink_200(wireMockServer, linkId, userId);

        var request = new CreateWebhookRequest(linkId, "https://hooks.example.com/callback", 100);
        var body = WebhookApiUtils.OK.create(request, userId, restTemplate);

        assertThat(body.linkId()).isEqualTo(linkId);
        assertThat(body.targetUrl()).isEqualTo("https://hooks.example.com/callback");
        assertThat(body.threshold()).isEqualTo(100);
        assertThat(body.active()).isTrue();
    }

    @Test
    void create_invalidUrl_returns400() {
        var userId = UUID.randomUUID();
        var request = new CreateWebhookRequest(UUID.randomUUID(), "not-a-url", 100);
        var response = WebhookApiUtils.Error.create(request, userId, restTemplate);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_negativeThreshold_returns400() {
        var userId = UUID.randomUUID();
        var request = new CreateWebhookRequest(UUID.randomUUID(), "https://hooks.example.com/cb", -1);
        var response = WebhookApiUtils.Error.create(request, userId, restTemplate);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void list_returnsUserConfigs() {
        var userId = UUID.randomUUID();
        var linkId = UUID.randomUUID();
        LinkServiceMocks.mockGetLink_200(wireMockServer, linkId, userId);

        WebhookApiUtils.OK.create(new CreateWebhookRequest(linkId, "https://hooks.example.com/cb", 50), userId, restTemplate);

        var list = WebhookApiUtils.OK.list(userId, restTemplate);
        assertThat(list).hasSize(1);
    }

    @Test
    void update_changesTargetUrl() {
        var userId = UUID.randomUUID();
        var linkId = UUID.randomUUID();
        LinkServiceMocks.mockGetLink_200(wireMockServer, linkId, userId);

        var created = WebhookApiUtils.OK.create(
                new CreateWebhookRequest(linkId, "https://hooks.example.com/old", 100), userId, restTemplate);

        var updated = WebhookApiUtils.OK.update(
                created.id(), new UpdateWebhookRequest("https://hooks.example.com/new", null), userId, restTemplate);

        assertThat(updated.targetUrl()).isEqualTo("https://hooks.example.com/new");
        assertThat(updated.active()).isTrue();
    }

    @Test
    void delete_removesConfig() {
        var userId = UUID.randomUUID();
        var linkId = UUID.randomUUID();
        LinkServiceMocks.mockGetLink_200(wireMockServer, linkId, userId);

        var created = WebhookApiUtils.OK.create(
                new CreateWebhookRequest(linkId, "https://hooks.example.com/cb", 100), userId, restTemplate);

        WebhookApiUtils.OK.delete(created.id(), userId, restTemplate);
        assertThat(configRepository.findById(created.id())).isEmpty();
    }

    @Test
    void get_otherUsersConfig_returns403() {
        var owner = UUID.randomUUID();
        var linkId = UUID.randomUUID();
        LinkServiceMocks.mockGetLink_200(wireMockServer, linkId, owner);

        var created = WebhookApiUtils.OK.create(
                new CreateWebhookRequest(linkId, "https://hooks.example.com/cb", 100), owner, restTemplate);

        var otherUser = UUID.randomUUID();
        var response = WebhookApiUtils.Error.get(created.id(), otherUser, restTemplate);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unauthenticated_returns401() {
        var response = WebhookApiUtils.Error.listNoAuth(restTemplate);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
