package com.smarturl.hub.auth.service;

import com.smarturl.hub.auth.config.JwtProperties;
import com.smarturl.hub.auth.error.TokenInvalidException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final List<String> DEFAULT_ROLES = List.of("ROLE_USER");

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final JwtProperties properties;
    private final Clock clock;

    public IssuedAccessToken issueAccessToken(UUID userId, String email) {
        UUID jti = UUID.randomUUID();
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.accessTokenTtl());

        String token = Jwts.builder()
                .issuer(properties.issuer())
                .subject(userId.toString())
                .id(jti.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("email", email)
                .claim("roles", DEFAULT_ROLES)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

        return new IssuedAccessToken(token, jti, expiresAt);
    }

    public ParsedAccessToken parseAndVerify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            UUID jti = UUID.fromString(claims.getId());
            String email = claims.get("email", String.class);
            return new ParsedAccessToken(userId, email, jti, claims.getExpiration().toInstant());
        } catch (ExpiredJwtException ex) {
            throw new TokenInvalidException("Token has expired", ex);
        } catch (IllegalArgumentException | JwtException ex) {
            throw new TokenInvalidException("Token is invalid", ex);
        }
    }

    public record IssuedAccessToken(String token, UUID jti, Instant expiresAt) {}

    public record ParsedAccessToken(UUID userId, String email, UUID jti, Instant expiresAt) {}
}
