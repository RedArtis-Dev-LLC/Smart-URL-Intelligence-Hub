package com.smarturl.hub.link.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.link.AbstractIntegrationTest;
import com.smarturl.hub.link.api.dto.CreateLinkRequest;
import com.smarturl.hub.link.api.utils.LinkApiUtils;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GetLinkIT extends AbstractIntegrationTest {

    @Test
    void get_happyPath_returnsLink() {
        //given
        var created = LinkApiUtils.OK.create(
                new CreateLinkRequest("https://example.com", null, null, null), userId, restTemplate);

        //when
        var fetched = LinkApiUtils.OK.get(created.id(), userId, restTemplate);

        //then
        assertThat(fetched.id()).isEqualTo(created.id());
        assertThat(fetched.shortCode()).isEqualTo(created.shortCode());
        assertThat(fetched.originalUrl()).isEqualTo(created.originalUrl());
    }

    @Test
    void get_unknownId_returns404() {
        //when
        var response = LinkApiUtils.Error.get(UUID.randomUUID(), userId, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getTitle()).isEqualTo("Not Found");
    }

    @Test
    void get_otherUsersLink_returns403() {
        //given
        var created = LinkApiUtils.OK.create(
                new CreateLinkRequest("https://example.com", null, null, null), userId, restTemplate);
        UUID otherUserId = UUID.randomUUID();

        //when
        var response = LinkApiUtils.Error.get(created.id(), otherUserId, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getTitle()).isEqualTo("Forbidden");
    }
}
