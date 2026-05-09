package com.smarturl.hub.link.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.link.AbstractIntegrationTest;
import com.smarturl.hub.link.api.dto.CreateLinkRequest;
import com.smarturl.hub.link.api.utils.LinkApiUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CreateLinkIT extends AbstractIntegrationTest {

    @Test
    void create_happyPath_returnsCreatedLink() {
        //given
        var request = new CreateLinkRequest("https://example.com/page", null, null, null);

        //when
        var response = LinkApiUtils.OK.create(request, userId, restTemplate);

        //then
        assertThat(response.id()).isNotNull();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.originalUrl()).isEqualTo("https://example.com/page");
        assertThat(response.shortCode()).isNotBlank().matches("^[A-Za-z0-9]{7}$");
        assertThat(response.customSlug()).isNull();
        assertThat(response.clickCount()).isZero();
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void create_withCustomSlug_usesCustomSlugAsShortCode() {
        //given
        var request = new CreateLinkRequest("https://example.com/page", "my-custom-slug", null, null);

        //when
        var response = LinkApiUtils.OK.create(request, userId, restTemplate);

        //then
        assertThat(response.shortCode()).isEqualTo("my-custom-slug");
        assertThat(response.customSlug()).isEqualTo("my-custom-slug");
    }

    @Test
    void create_withExpiresAtAndMaxClicks_persistsConstraints() {
        //given
        Instant expires = Instant.now().plus(7, ChronoUnit.DAYS);
        var request = new CreateLinkRequest("https://example.com/page", null, expires, 100L);

        //when
        var response = LinkApiUtils.OK.create(request, userId, restTemplate);

        //then
        assertThat(response.expiresAt()).isEqualTo(expires);
        assertThat(response.maxClicks()).isEqualTo(100L);
    }

    @Test
    void create_persistsToDatabase() {
        //given
        var request = new CreateLinkRequest("https://example.com/page", null, null, null);

        //when
        var response = LinkApiUtils.OK.create(request, userId, restTemplate);

        //then
        var stored = linkRepository.findById(response.id()).orElseThrow();
        assertThat(stored.getShortCode()).isEqualTo(response.shortCode());
        assertThat(stored.getUserId()).isEqualTo(userId);
        assertThat(stored.isActive()).isTrue();
    }

    @Test
    void create_duplicateCustomSlug_returns409() {
        //given
        var first = new CreateLinkRequest("https://example.com/a", "duplicate-slug", null, null);
        LinkApiUtils.OK.create(first, userId, restTemplate);

        //when
        var second = new CreateLinkRequest("https://example.com/b", "duplicate-slug", null, null);
        var response = LinkApiUtils.Error.create(second, userId, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void create_invalidUrl_returns400() {
        //given
        var request = new CreateLinkRequest("not-a-url", null, null, null);

        //when
        var response = LinkApiUtils.Error.create(request, userId, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
    }

    @Test
    void create_blankUrl_returns400() {
        //given
        var request = new CreateLinkRequest("", null, null, null);

        //when
        var response = LinkApiUtils.Error.create(request, userId, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_invalidCustomSlugCharacters_returns400() {
        //given
        var request = new CreateLinkRequest("https://example.com", "invalid slug!", null, null);

        //when
        var response = LinkApiUtils.Error.create(request, userId, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_zeroOrNegativeMaxClicks_returns400() {
        //given
        var request = new CreateLinkRequest("https://example.com", null, null, 0L);

        //when
        var response = LinkApiUtils.Error.create(request, userId, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_noAuth_returns401() {
        //given
        var request = new CreateLinkRequest("https://example.com", null, null, null);

        //when
        var response = LinkApiUtils.Error.createNoAuth(request, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
