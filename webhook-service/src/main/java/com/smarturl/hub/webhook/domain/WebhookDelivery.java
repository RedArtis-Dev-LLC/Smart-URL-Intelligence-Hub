package com.smarturl.hub.webhook.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "webhook_deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDelivery {

    @Id
    private UUID id;

    @Column(name = "config_id", nullable = false)
    private UUID configId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 1;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
