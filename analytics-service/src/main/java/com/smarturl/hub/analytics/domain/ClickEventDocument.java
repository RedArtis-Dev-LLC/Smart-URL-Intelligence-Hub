package com.smarturl.hub.analytics.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "click_events")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClickEventDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private UUID eventId;

    @Indexed
    private UUID linkId;

    private UUID userId;

    private String shortCode;

    @Indexed
    private Instant timestamp;

    private String ip;
    private String country;
    private String city;
    private String deviceType;
    private String os;
    private String browser;
    private String referrer;
}
