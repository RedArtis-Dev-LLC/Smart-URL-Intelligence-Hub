package com.smarturl.hub.common.security;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email, List<String> roles) {

    public AuthenticatedUser {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
