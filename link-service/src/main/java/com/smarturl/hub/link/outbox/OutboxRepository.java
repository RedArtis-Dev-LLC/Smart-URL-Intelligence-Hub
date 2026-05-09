package com.smarturl.hub.link.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    long countByPublishedFalse();

    @Query(
            value = """
                    SELECT * FROM outbox_events
                    WHERE published = false
                    ORDER BY created_at
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true)
    List<OutboxEvent> findUnpublishedForUpdate(@Param("limit") int limit);

    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.published = true AND e.createdAt < :cutoff")
    int deletePublishedBefore(@Param("cutoff") Instant cutoff);
}
