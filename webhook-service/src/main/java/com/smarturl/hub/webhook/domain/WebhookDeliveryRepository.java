package com.smarturl.hub.webhook.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    boolean existsByEventId(UUID eventId);
}
