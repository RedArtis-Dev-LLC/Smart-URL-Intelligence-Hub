package com.smarturl.hub.link.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.link.AbstractIntegrationTest;
import com.smarturl.hub.link.api.dto.CreateLinkRequest;
import com.smarturl.hub.link.api.utils.LinkApiUtils;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class DeleteLinkIT extends AbstractIntegrationTest {

    @Test
    void delete_happyPath_marksLinkInactive() {
        //given
        var created = LinkApiUtils.OK.create(
                new CreateLinkRequest("https://example.com", null, null, null), userId, restTemplate);

        //when
        LinkApiUtils.OK.delete(created.id(), userId, restTemplate);

        //then
        var stored = linkRepository.findById(created.id()).orElseThrow();
        assertThat(stored.isActive()).isFalse();
    }

    @Test
    void delete_unknownId_returns404() {
        //when
        var response = LinkApiUtils.Error.delete(UUID.randomUUID(), userId, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_thenGet_returns404() {
        //given
        var created = LinkApiUtils.OK.create(
                new CreateLinkRequest("https://example.com", null, null, null), userId, restTemplate);
        LinkApiUtils.OK.delete(created.id(), userId, restTemplate);

        //when
        var response = LinkApiUtils.Error.get(created.id(), userId, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_otherUsersLink_returns403() {
        //given
        var created = LinkApiUtils.OK.create(
                new CreateLinkRequest("https://example.com", null, null, null), userId, restTemplate);
        UUID otherUserId = UUID.randomUUID();

        //when
        var response = LinkApiUtils.Error.delete(created.id(), otherUserId, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
