package com.smarturl.hub.link.service;

import com.smarturl.hub.link.config.LinkProperties;
import com.smarturl.hub.link.domain.Link;
import com.smarturl.hub.link.domain.LinkRepository;
import com.smarturl.hub.link.error.LinkGoneException;
import com.smarturl.hub.link.error.LinkNotFoundException;
import com.smarturl.hub.link.outbox.OutboxWriter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedirectService {

    private static final String REDIRECT_COUNTER = "link.redirects.total";

    private final LinkRepository linkRepository;
    private final OutboxWriter outboxWriter;
    private final MeterRegistry meterRegistry;
    private final LinkProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public String resolveAndRecord(String shortCode, ClickContext context) {
        int maxRetries = properties.redirect().maxOptimisticLockRetries();
        ObjectOptimisticLockingFailureException last = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return transactionTemplate.execute(_ -> doResolveAndRecord(shortCode, context));
            } catch (ObjectOptimisticLockingFailureException ex) {
                last = ex;
                log.debug("Optimistic lock retry on redirect [shortCode={}, attempt={}/{}]",
                        shortCode, attempt, maxRetries);
            }
        }
        log.warn("Exhausted optimistic-lock retries on redirect [shortCode={}]", shortCode);
        throw last;
    }

    private String doResolveAndRecord(String shortCode, ClickContext context) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException(shortCode));

        ensureUsable(link, clock);

        link.setClickCount(link.getClickCount() + 1);
        linkRepository.saveAndFlush(link);

        ClickEventPayload payload = new ClickEventPayload(
                UUID.randomUUID(),
                link.getId(),
                link.getUserId(),
                link.getShortCode(),
                link.getOriginalUrl(),
                Instant.now(clock),
                context.ip(),
                context.userAgent(),
                context.referrer());
        outboxWriter.write(link.getId(), OutboxWriter.CLICK_EVENT_TYPE, payload);

        meterRegistry.counter(REDIRECT_COUNTER).increment();
        return link.getOriginalUrl();
    }

    private static void ensureUsable(Link link, Clock clock) {
        if (!link.isActive()) {
            throw new LinkGoneException("Link is inactive");
        }
        if (link.getExpiresAt() != null && Instant.now(clock).isAfter(link.getExpiresAt())) {
            throw new LinkGoneException("Link has expired");
        }
        if (link.getMaxClicks() != null && link.getClickCount() >= link.getMaxClicks()) {
            throw new LinkGoneException("Link has reached maximum clicks");
        }
    }
}
