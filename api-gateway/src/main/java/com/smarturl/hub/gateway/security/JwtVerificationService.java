package com.smarturl.hub.gateway.security;

import com.smarturl.hub.gateway.config.GatewayJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
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
public class JwtVerificationService {

    private final RSAPublicKey publicKey;
    private final GatewayJwtProperties properties;
    private final Clock clock;

    @SuppressWarnings("unchecked")
    public ParsedToken verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(properties.issuer())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            UUID jti = UUID.fromString(claims.getId());
            String email = claims.get("email", String.class);
            List<String> roles = claims.get("roles", List.class);
            return new ParsedToken(
                    userId,
                    email,
                    roles == null ? List.of() : List.copyOf(roles),
                    jti,
                    claims.getExpiration().toInstant());
        } catch (ExpiredJwtException ex) {
            throw new TokenInvalidException("Token has expired", ex);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new TokenInvalidException("Token is invalid", ex);
        }
    }

    public record ParsedToken(UUID userId, String email, List<String> roles, UUID jti, Instant expiresAt) {}
}
