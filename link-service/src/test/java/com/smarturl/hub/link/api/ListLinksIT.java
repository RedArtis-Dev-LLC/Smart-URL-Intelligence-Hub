package com.smarturl.hub.link.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.link.AbstractIntegrationTest;
import com.smarturl.hub.link.api.dto.CreateLinkRequest;
import com.smarturl.hub.link.api.utils.LinkApiUtils;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListLinksIT extends AbstractIntegrationTest {

    @Test
    void list_happyPath_returnsOnlyOwnedLinks() {
        //given
        UUID otherUserId = UUID.randomUUID();
        LinkApiUtils.OK.create(new CreateLinkRequest("https://example.com/a", null, null, null), userId, restTemplate);
        LinkApiUtils.OK.create(new CreateLinkRequest("https://example.com/b", null, null, null), userId, restTemplate);
        LinkApiUtils.OK.create(new CreateLinkRequest("https://example.com/c", null, null, null), otherUserId, restTemplate);

        //when
        var result = LinkApiUtils.OK.list(0, 20, userId, restTemplate);

        //then
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content()).hasSize(2);
        assertThat(result.content()).allMatch(l -> l.userId().equals(userId));
    }

    @Test
    void list_excludesSoftDeletedLinks() {
        //given
        var first = LinkApiUtils.OK.create(
                new CreateLinkRequest("https://example.com/a", null, null, null), userId, restTemplate);
        LinkApiUtils.OK.create(new CreateLinkRequest("https://example.com/b", null, null, null), userId, restTemplate);
        LinkApiUtils.OK.delete(first.id(), userId, restTemplate);

        //when
        var result = LinkApiUtils.OK.list(0, 20, userId, restTemplate);

        //then
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).noneMatch(l -> l.id().equals(first.id()));
    }

    @Test
    void list_paging_returnsCorrectPage() {
        //given
        for (int i = 0; i < 5; i++) {
            LinkApiUtils.OK.create(
                    new CreateLinkRequest("https://example.com/" + i, null, null, null), userId, restTemplate);
        }

        //when
        var page0 = LinkApiUtils.OK.list(0, 2, userId, restTemplate);
        var page1 = LinkApiUtils.OK.list(1, 2, userId, restTemplate);
        var page2 = LinkApiUtils.OK.list(2, 2, userId, restTemplate);

        //then
        assertThat(page0.content()).hasSize(2);
        assertThat(page1.content()).hasSize(2);
        assertThat(page2.content()).hasSize(1);
        assertThat(page0.totalElements()).isEqualTo(5);
        assertThat(page0.totalPages()).isEqualTo(3);
    }

    @Test
    void list_noLinks_returnsEmptyPage() {
        //when
        var result = LinkApiUtils.OK.list(0, 20, userId, restTemplate);

        //then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }
}
