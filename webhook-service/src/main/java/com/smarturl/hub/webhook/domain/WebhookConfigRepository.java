package com.smarturl.hub.webhook.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, UUID> {

    List<WebhookConfig> findByUserId(UUID userId);

    List<WebhookConfig> findByLinkIdAndActiveTrue(UUID linkId);
}
