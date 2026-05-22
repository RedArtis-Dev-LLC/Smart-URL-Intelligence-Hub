package com.smarturl.hub.analytics.service;

import com.smarturl.hub.analytics.config.AnalyticsProperties;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClickCounterService {

    private final StringRedisTemplate redis;
    private final AnalyticsProperties properties;
    private final Clock clock;

    public long incrementTotal(UUID linkId) {
        String key = totalKey(linkId);
        Long value = redis.opsForValue().increment(key);
        return value == null ? 0L : value;
    }

    public void addUniqueVisitor(UUID linkId, String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        String key = uniqueKey(linkId, today());
        redis.opsForHyperLogLog().add(key, ip);
    }

    public long getTotal(UUID linkId) {
        String value = redis.opsForValue().get(totalKey(linkId));
        return value == null ? 0L : Long.parseLong(value);
    }

    public long countUnique(UUID linkId, LocalDate date) {
        Long count = redis.opsForHyperLogLog().size(uniqueKey(linkId, date));
        return count == null ? 0L : count;
    }

    private String totalKey(UUID linkId) {
        return properties.redis().clickTotalPrefix() + linkId;
    }

    private String uniqueKey(UUID linkId, LocalDate date) {
        return properties.redis().clickUniquePrefix() + linkId + ":" + date;
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
