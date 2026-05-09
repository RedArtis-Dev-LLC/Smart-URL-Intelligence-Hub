package com.smarturl.hub.link.api.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PagedLinksResponse(
        List<LinkResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static PagedLinksResponse from(Page<LinkResponse> page) {
        return new PagedLinksResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
