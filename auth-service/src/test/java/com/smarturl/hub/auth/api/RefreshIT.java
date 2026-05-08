package com.smarturl.hub.auth.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.auth.AbstractIntegrationTest;
import com.smarturl.hub.auth.api.dto.RefreshRequest;
import com.smarturl.hub.auth.api.dto.RegisterRequest;
import com.smarturl.hub.auth.api.utils.AuthApiUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RefreshIT extends AbstractIntegrationTest {

    @Test
    void refresh_happyPath_returnsNewTokens() {
        //given
        var registered = AuthApiUtils.OK.register(new RegisterRequest("alice@example.com", "hunter22pass"), restTemplate);

        //when
        var result = AuthApiUtils.OK.refresh(new RefreshRequest(registered.refreshToken()), restTemplate);

        //then
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void refresh_happyPath_rotatesRefreshToken() {
        //given
        var registered = AuthApiUtils.OK.register(new RegisterRequest("alice@example.com", "hunter22pass"), restTemplate);
        var originalRefresh = registered.refreshToken();

        //when
        var result = AuthApiUtils.OK.refresh(new RefreshRequest(originalRefresh), restTemplate);

        //then
        assertThat(result.refreshToken()).isNotEqualTo(originalRefresh);
    }

    @Test
    void refresh_reusedToken_returns401() {
        //given
        var registered = AuthApiUtils.OK.register(new RegisterRequest("alice@example.com", "hunter22pass"), restTemplate);
        var refreshToken = registered.refreshToken();
        AuthApiUtils.OK.refresh(new RefreshRequest(refreshToken), restTemplate);

        //when
        var response = AuthApiUtils.Error.refresh(new RefreshRequest(refreshToken), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_invalidToken_returns401() {
        //when
        var response = AuthApiUtils.Error.refresh(new RefreshRequest("not-a-real-token"), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_blankToken_returns400() {
        //when
        var response = AuthApiUtils.Error.refresh(new RefreshRequest(""), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void refresh_canUseNewTokenAfterRotation() {
        //given
        var registered = AuthApiUtils.OK.register(new RegisterRequest("alice@example.com", "hunter22pass"), restTemplate);
        var rotated = AuthApiUtils.OK.refresh(new RefreshRequest(registered.refreshToken()), restTemplate);

        //when
        var result = AuthApiUtils.OK.refresh(new RefreshRequest(rotated.refreshToken()), restTemplate);

        //then
        assertThat(result.accessToken()).isNotBlank();
    }
}
