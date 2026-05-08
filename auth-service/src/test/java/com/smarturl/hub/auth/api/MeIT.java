package com.smarturl.hub.auth.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.auth.AbstractIntegrationTest;
import com.smarturl.hub.auth.api.dto.RegisterRequest;
import com.smarturl.hub.auth.api.utils.AuthApiUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class MeIT extends AbstractIntegrationTest {

    @Test
    void me_happyPath_returnsUserInfo() {
        //given
        var registered = AuthApiUtils.OK.register(new RegisterRequest("alice@example.com", "hunter22pass"), restTemplate);

        //when
        var result = AuthApiUtils.OK.me(registered.accessToken(), restTemplate);

        //then
        assertThat(result.email()).isEqualTo("alice@example.com");
        assertThat(result.userId()).isNotNull();
    }

    @Test
    void me_withoutBearerToken_returns401() {
        //when
        var response = AuthApiUtils.Error.meNoAuth(restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_invalidToken_returns401() {
        //when
        var response = AuthApiUtils.Error.me("not-a-valid-jwt", restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_revokedToken_returns401() {
        //given
        var registered = AuthApiUtils.OK.register(new RegisterRequest("alice@example.com", "hunter22pass"), restTemplate);
        AuthApiUtils.OK.logout(registered.accessToken(), restTemplate);

        //when
        var response = AuthApiUtils.Error.me(registered.accessToken(), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
