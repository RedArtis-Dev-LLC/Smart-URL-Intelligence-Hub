package com.smarturl.hub.auth.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.auth.AbstractIntegrationTest;
import com.smarturl.hub.auth.api.dto.LoginRequest;
import com.smarturl.hub.auth.api.dto.RegisterRequest;
import com.smarturl.hub.auth.api.utils.AuthApiUtils;
import io.jsonwebtoken.Jwts;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class LoginIT extends AbstractIntegrationTest {

    @Autowired RSAPublicKey rsaPublicKey;

    @Test
    void login_happyPath_returnsTokens() {
        //given
        AuthApiUtils.OK.register(new RegisterRequest("alice@example.com", "hunter22pass"), restTemplate);

        //when
        var result = AuthApiUtils.OK.login(new LoginRequest("alice@example.com", "hunter22pass"), restTemplate);

        //then
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_happyPath_accessTokenContainsCorrectClaims() {
        //given
        AuthApiUtils.OK.register(new RegisterRequest("alice@example.com", "hunter22pass"), restTemplate);

        //when
        var result = AuthApiUtils.OK.login(new LoginRequest("alice@example.com", "hunter22pass"), restTemplate);

        //then
        var claims = Jwts.parser().verifyWith(rsaPublicKey).build()
                .parseSignedClaims(result.accessToken()).getPayload();
        assertThat(claims.get("email", String.class)).isEqualTo("alice@example.com");
        assertThat(claims.get("roles", java.util.List.class)).containsExactly("ROLE_USER");
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    void login_wrongPassword_returns401() {
        //given
        AuthApiUtils.OK.register(new RegisterRequest("dave@example.com", "hunter22pass"), restTemplate);

        //when
        var response = AuthApiUtils.Error.login(new LoginRequest("dave@example.com", "WRONGpass1"), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_nonExistentUser_returns401() {
        //when
        var response = AuthApiUtils.Error.login(new LoginRequest("nobody@example.com", "hunter22pass"), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_blankEmail_returns400() {
        //when
        var response = AuthApiUtils.Error.login(new LoginRequest("", "hunter22pass"), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_blankPassword_returns400() {
        //when
        var response = AuthApiUtils.Error.login(new LoginRequest("alice@example.com", ""), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_emailIsCaseInsensitive() {
        //given
        AuthApiUtils.OK.register(new RegisterRequest("alice@example.com", "hunter22pass"), restTemplate);

        //when
        var result = AuthApiUtils.OK.login(new LoginRequest("Alice@Example.COM", "hunter22pass"), restTemplate);

        //then
        assertThat(result.accessToken()).isNotBlank();
    }
}
