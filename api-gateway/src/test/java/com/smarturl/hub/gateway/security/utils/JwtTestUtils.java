package com.smarturl.hub.gateway.security.utils;

import io.jsonwebtoken.Jwts;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JwtTestUtils {

    public static final String DEFAULT_ISSUER = "smart-url-hub-auth-test";
    public static final List<String> DEFAULT_ROLES = List.of("ROLE_USER");

    public static String issueValidToken(UUID userId, String email, RSAPrivateKey key) {
        Instant now = Instant.now();
        return issue(userId, UUID.randomUUID(), email, DEFAULT_ROLES,
                now, now.plusSeconds(900), DEFAULT_ISSUER, key);
    }

    public static String issueValidTokenWithJti(UUID userId, UUID jti, String email, RSAPrivateKey key) {
        Instant now = Instant.now();
        return issue(userId, jti, email, DEFAULT_ROLES,
                now, now.plusSeconds(900), DEFAULT_ISSUER, key);
    }

    public static String issueExpiredToken(UUID userId, String email, RSAPrivateKey key) {
        Instant pastIssued = Instant.now().minusSeconds(7200);
        Instant pastExpired = Instant.now().minusSeconds(60);
        return issue(userId, UUID.randomUUID(), email, DEFAULT_ROLES,
                pastIssued, pastExpired, DEFAULT_ISSUER, key);
    }

    public static String issueWithIssuer(UUID userId, String email, String issuer, RSAPrivateKey key) {
        Instant now = Instant.now();
        return issue(userId, UUID.randomUUID(), email, DEFAULT_ROLES,
                now, now.plusSeconds(900), issuer, key);
    }

    public static String issue(
            UUID userId,
            UUID jti,
            String email,
            List<String> roles,
            Instant issuedAt,
            Instant expiresAt,
            String issuer,
            RSAPrivateKey privateKey) {
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .id(jti.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("email", email)
                .claim("roles", roles)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }
}
