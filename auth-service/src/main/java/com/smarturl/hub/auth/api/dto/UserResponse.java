package com.smarturl.hub.auth.api.dto;

import java.util.UUID;

public record UserResponse(UUID userId, String email) {
}
