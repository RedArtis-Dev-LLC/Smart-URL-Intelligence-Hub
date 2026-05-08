package com.smarturl.hub.auth.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.auth.AbstractIntegrationTest;
import com.smarturl.hub.auth.api.dto.RegisterRequest;
import com.smarturl.hub.auth.api.utils.AuthApiUtils;
import io.jsonwebtoken.Jwts;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class LogoutIT extends AbstractIntegrationTest {

    @Autowired RSAPublicKey rsaPublicKey;

    @Test
    void logout_happyPath_returns204() {
        //given
        var registered = AuthApiUtils.OK.register(new RegisterRequest("alice@example.com", "hunter22pass"), restTemplate);

        //when
        AuthApiUtils.OK.logout(registered.accessToken(), restTemplate);

        //then (no error thrown by OK.logout means 204 was returned)
    }

    @Test
    void logout_happyPath_revokesTokenInRedis() {
        //given
        var registered = AuthApiUtils.OK.register(new RegisterRequest("alice@example.com", "hunter22pass"), restTemplate);
        var claims = Jwts.parser().verifyWith(rsaPublicKey).build()
                .parseSignedClaims(registered.accessToken()).getPayload();
        var jti = UUID.fromString(claims.getId());

        //when
        AuthApiUtils.OK.logout(registered.accessToken(), restTemplate);

        //then
        assertThat(redis.hasKey("revoked:" + jti)).isTrue();
    }

    @Test
    void logout_alreadyRevokedToken_stillReturns204() {
        //given
        var registered = AuthApiUtils.OK.register(new RegisterRequest("alice@example.com", "hunter22pass"), restTemplate);
        AuthApiUtils.OK.logout(registered.accessToken(), restTemplate);

        //when
        AuthApiUtils.OK.logout(registered.accessToken(), restTemplate);

        //then (idempotent - OK.logout asserts 204)
    }

    @Test
    void logout_missingAuthorizationHeader_returns401() {
        //when
        var response = AuthApiUtils.Error.logoutNoAuth(restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_invalidToken_returns401() {
        //when
        var response = AuthApiUtils.Error.logout("not-a-valid-jwt", restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
