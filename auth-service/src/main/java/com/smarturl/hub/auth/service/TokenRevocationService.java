package com.smarturl.hub.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private static final String KEY_PREFIX = "auth:revoked:";

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    public void revoke(UUID jti, Instant expiresAt) {
        Duration ttl = Duration.between(clock.instant(), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", ttl);
    }

    public boolean isRevoked(UUID jti) {
        Boolean exists = redisTemplate.hasKey(KEY_PREFIX + jti);
        return Boolean.TRUE.equals(exists);
    }
}
