package com.smarturl.hub.link.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.hibernate.validator.constraints.URL;

public record CreateLinkRequest(
        @NotBlank
        @URL
        @Size(max = 2048)
        String originalUrl,

        @Pattern(regexp = "^[A-Za-z0-9_-]{3,50}$",
                message = "must be 3-50 characters of letters, digits, hyphens, or underscores")
        String customSlug,

        Instant expiresAt,

        @Min(value = 1, message = "must be at least 1")
        Long maxClicks) {
}
