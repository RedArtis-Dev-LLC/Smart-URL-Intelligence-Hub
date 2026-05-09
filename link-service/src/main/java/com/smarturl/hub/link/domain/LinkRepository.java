package com.smarturl.hub.link.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkRepository extends JpaRepository<Link, UUID> {

    Optional<Link> findByShortCode(String shortCode);

    Page<Link> findByUserIdAndActiveTrue(UUID userId, Pageable pageable);
}
