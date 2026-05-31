package com.smarturl.hub.webhook.service;

import com.smarturl.hub.webhook.api.dto.CreateWebhookRequest;
import com.smarturl.hub.webhook.api.dto.UpdateWebhookRequest;
import com.smarturl.hub.webhook.api.dto.WebhookConfigResponse;
import com.smarturl.hub.webhook.api.dto.WebhookThresholdResponse;
import com.smarturl.hub.webhook.domain.WebhookConfig;
import com.smarturl.hub.webhook.domain.WebhookConfigRepository;
import com.smarturl.hub.webhook.feign.LinkServiceClient;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class WebhookConfigService {

    private final WebhookConfigRepository repository;
    private final LinkServiceClient linkServiceClient;
    private final Clock clock;

    @Transactional
    public WebhookConfigResponse create(UUID userId, CreateWebhookRequest request) {
        verifyOwnership(request.linkId(), userId);

        var config = WebhookConfig.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .linkId(request.linkId())
                .targetUrl(request.targetUrl())
                .threshold(request.threshold())
                .createdAt(Instant.now(clock))
                .build();
        return WebhookConfigResponse.from(repository.save(config));
    }

    @Transactional(readOnly = true)
    public List<WebhookConfigResponse> listByUser(UUID userId) {
        return repository.findByUserId(userId).stream()
                .map(WebhookConfigResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WebhookConfigResponse get(UUID id, UUID userId) {
        var config = findOwnedConfig(id, userId);
        return WebhookConfigResponse.from(config);
    }

    @Transactional
    public WebhookConfigResponse update(UUID id, UUID userId, UpdateWebhookRequest request) {
        var config = findOwnedConfig(id, userId);
        if (request.targetUrl() != null) {
            config.setTargetUrl(request.targetUrl());
        }
        if (request.active() != null) {
            config.setActive(request.active());
        }
        return WebhookConfigResponse.from(repository.save(config));
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        var config = findOwnedConfig(id, userId);
        repository.delete(config);
    }

    @Transactional(readOnly = true)
    public List<WebhookThresholdResponse> getThresholdsByLinkId(UUID linkId) {
        return repository.findByLinkIdAndActiveTrue(linkId).stream()
                .map(c -> new WebhookThresholdResponse(c.getId(), c.getThreshold()))
                .toList();
    }

    private WebhookConfig findOwnedConfig(UUID id, UUID userId) {
        var config = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Webhook config not found"));
        if (!config.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return config;
    }

    private void verifyOwnership(UUID linkId, UUID userId) {
        try {
            var link = linkServiceClient.getLink(linkId, userId.toString());
            if (!link.userId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this link");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException _) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to verify link ownership");
        }
    }
}
