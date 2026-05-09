package com.smarturl.hub.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.jwt")
public record GatewayJwtProperties(String publicKeyPath, String issuer) {
}
