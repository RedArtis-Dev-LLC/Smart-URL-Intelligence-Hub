package com.smarturl.hub.gateway.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

@Slf4j
@Configuration
@EnableConfigurationProperties(GatewayJwtProperties.class)
@RequiredArgsConstructor
public class RsaPublicKeyConfig {

    private final ResourceLoader resourceLoader;
    private final GatewayJwtProperties properties;

    @Bean
    public RSAPublicKey rsaPublicKey() throws Exception {
        String pem = read(properties.publicKeyPath());
        byte[] der = decodePem(pem);
        var spec = new X509EncodedKeySpec(der);
        log.info("Loaded RSA public key from {}", properties.publicKeyPath());
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private String read(String location) throws IOException {
        try (var in = resourceLoader.getResource(location).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
    }

    private static byte[] decodePem(String pem) {
        String begin = "-----BEGIN PUBLIC KEY-----";
        String end = "-----END PUBLIC KEY-----";
        int start = pem.indexOf(begin);
        int stop = pem.indexOf(end);
        if (start < 0 || stop < 0) {
            throw new IllegalStateException("PEM does not contain a PUBLIC KEY block");
        }
        String body = pem.substring(start + begin.length(), stop).replaceAll("\\s+", "");
        return Base64.getDecoder().decode(body);
    }
}
