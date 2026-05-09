package com.smarturl.hub.link.service;

import com.smarturl.hub.link.api.dto.CreateLinkRequest;
import com.smarturl.hub.link.api.dto.LinkResponse;
import com.smarturl.hub.link.api.dto.PagedLinksResponse;
import com.smarturl.hub.link.config.LinkProperties;
import com.smarturl.hub.link.domain.Link;
import com.smarturl.hub.link.domain.LinkRepository;
import com.smarturl.hub.link.error.LinkAccessDeniedException;
import com.smarturl.hub.link.error.LinkNotFoundException;
import com.smarturl.hub.link.error.ShortCodeUnavailableException;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final LinkProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public LinkResponse create(UUID userId, CreateLinkRequest request) {
        String customSlug = trimToNull(request.customSlug());
        if (customSlug != null) {
            return persist(userId, request, customSlug, customSlug, true);
        }
        int maxAttempts = properties.shortCode().maxGenerationRetries();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String generated = shortCodeGenerator.generate();
            try {
                return persist(userId, request, generated, null, false);
            } catch (DataIntegrityViolationException _) {
                log.debug("Short-code race on save [code={}, attempt={}/{}]", generated, attempt, maxAttempts);
            }
        }
        throw new ShortCodeUnavailableException(
                "Could not allocate a unique short code after " + maxAttempts + " attempts");
    }

    @Transactional(readOnly = true)
    public LinkResponse get(UUID linkId, UUID requestingUserId) {
        Link link = loadOwned(linkId, requestingUserId);
        return LinkResponse.from(link);
    }

    @Transactional(readOnly = true)
    public PagedLinksResponse list(UUID userId, int page, int size) {
        Page<Link> result = linkRepository.findByUserIdAndActiveTrue(userId, PageRequest.of(page, size));
        return PagedLinksResponse.from(result.map(LinkResponse::from));
    }

    @Transactional
    public void delete(UUID linkId, UUID requestingUserId) {
        Link link = loadOwned(linkId, requestingUserId);
        link.setActive(false);
        log.info("Link soft-deleted [linkId={}, userId={}]", link.getId(), requestingUserId);
    }

    private LinkResponse persist(UUID userId, CreateLinkRequest request,
                                 String shortCode, String customSlug, boolean customSlugProvided) {
        try {
            return transactionTemplate.execute(_ -> {
                Link link = Link.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .originalUrl(request.originalUrl())
                        .shortCode(shortCode)
                        .customSlug(customSlug)
                        .expiresAt(request.expiresAt())
                        .maxClicks(request.maxClicks())
                        .clickCount(0L)
                        .active(true)
                        .createdAt(clock.instant())
                        .build();
                linkRepository.saveAndFlush(link);
                log.info("Link created [linkId={}, shortCode={}, userId={}]",
                        link.getId(), shortCode, userId);
                return LinkResponse.from(link);
            });
        } catch (DataIntegrityViolationException ex) {
            if (customSlugProvided) {
                throw new ShortCodeUnavailableException("Short code '" + shortCode + "' is already in use");
            }
            throw ex;
        }
    }

    private Link loadOwned(UUID linkId, UUID userId) {
        Link link = linkRepository.findById(linkId)
                .filter(Link::isActive)
                .orElseThrow(() -> new LinkNotFoundException(linkId.toString()));
        if (!link.getUserId().equals(userId)) {
            throw new LinkAccessDeniedException();
        }
        return link;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
