package com.smarturl.hub.auth.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
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
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class RsaKeyConfig {

    private final ResourceLoader resourceLoader;
    private final JwtProperties properties;

    @Bean
    public RSAPrivateKey rsaPrivateKey() throws Exception {
        byte[] der = decodePem(read(properties.privateKeyPath()), "PRIVATE KEY");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        log.info("Loaded RSA private key from {}", properties.privateKeyPath());
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    @Bean
    public RSAPublicKey rsaPublicKey() throws Exception {
        byte[] der = decodePem(read(properties.publicKeyPath()), "PUBLIC KEY");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        log.info("Loaded RSA public key from {}", properties.publicKeyPath());
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    @Bean
    public String publicKeyPem() throws IOException {
        return read(properties.publicKeyPath());
    }

    private String read(String location) throws IOException {
        try (var in = resourceLoader.getResource(location).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
    }

    private static byte[] decodePem(String pem, String label) {
        String begin = "-----BEGIN " + label + "-----";
        String end = "-----END " + label + "-----";
        int start = pem.indexOf(begin);
        int stop = pem.indexOf(end);
        if (start < 0 || stop < 0) {
            throw new IllegalStateException("PEM does not contain " + label);
        }
        String body = pem.substring(start + begin.length(), stop)
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(body);
    }
}
