package com.smarturl.hub.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.jwt")
public record JwtProperties(
        String privateKeyPath,
        String publicKeyPath,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        String issuer) {
}
