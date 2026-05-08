package com.smarturl.hub.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarturl.hub.auth.config.JwtProperties;
import com.smarturl.hub.auth.error.TokenInvalidException;
import com.smarturl.hub.auth.service.JwtService.IssuedAccessToken;
import com.smarturl.hub.auth.service.JwtService.ParsedAccessToken;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    static RSAPrivateKey privateKey;
    static RSAPublicKey publicKey;

    @BeforeAll
    static void setUpKeys() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        privateKey = (RSAPrivateKey) pair.getPrivate();
        publicKey = (RSAPublicKey) pair.getPublic();
    }

    @Test
    void issuedToken_canBeParsedAndCarriesClaims() {
        Instant now = Instant.parse("2026-05-08T10:00:00Z");
        JwtService svc = service(Clock.fixed(now, ZoneOffset.UTC));

        UUID userId = UUID.randomUUID();
        IssuedAccessToken issued = svc.issueAccessToken(userId, "alice@example.com");

        ParsedAccessToken parsed = svc.parseAndVerify(issued.token());
        assertThat(parsed.userId()).isEqualTo(userId);
        assertThat(parsed.email()).isEqualTo("alice@example.com");
        assertThat(parsed.jti()).isEqualTo(issued.jti());
        assertThat(parsed.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(15)));
    }

    @Test
    void parseAndVerify_rejectsExpiredToken() {
        Instant issuedAt = Instant.parse("2026-05-08T10:00:00Z");
        JwtService issuer = service(Clock.fixed(issuedAt, ZoneOffset.UTC));
        IssuedAccessToken issued = issuer.issueAccessToken(UUID.randomUUID(), "x@y.z");

        JwtService later = service(Clock.fixed(issuedAt.plus(Duration.ofHours(1)), ZoneOffset.UTC));
        assertThatThrownBy(() -> later.parseAndVerify(issued.token()))
                .isInstanceOf(TokenInvalidException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void parseAndVerify_rejectsGarbage() {
        JwtService svc = service(Clock.systemUTC());
        assertThatThrownBy(() -> svc.parseAndVerify("not-a-jwt"))
                .isInstanceOf(TokenInvalidException.class);
    }

    private static JwtService service(Clock clock) {
        JwtProperties props = new JwtProperties(
                null, null,
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                "smart-url-hub-auth-test");
        return new JwtService(privateKey, publicKey, props, clock);
    }
}
