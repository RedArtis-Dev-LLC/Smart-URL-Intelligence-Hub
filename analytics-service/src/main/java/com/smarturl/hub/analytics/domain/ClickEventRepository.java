package com.smarturl.hub.analytics.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClickEventRepository extends MongoRepository<ClickEventDocument, String>,
        ClickEventAggregationRepository {

    boolean existsByEventId(UUID eventId);

    long countByLinkId(UUID linkId);

    long countByLinkIdAndTimestampBetween(UUID linkId, Instant from, Instant to);
}
