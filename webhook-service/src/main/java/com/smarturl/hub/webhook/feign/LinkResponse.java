package com.smarturl.hub.webhook.feign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LinkResponse(UUID id, UUID userId) {
}
