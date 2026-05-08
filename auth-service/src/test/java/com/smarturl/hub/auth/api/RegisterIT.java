package com.smarturl.hub.auth.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.auth.AbstractIntegrationTest;
import com.smarturl.hub.auth.api.dto.RegisterRequest;
import com.smarturl.hub.auth.api.utils.AuthApiUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RegisterIT extends AbstractIntegrationTest {

    @Test
    void register_happyPath_returnsTokens() {
        //given
        var request = new RegisterRequest("alice@example.com", "hunter22pass");

        //when
        var result = AuthApiUtils.OK.register(request, restTemplate);

        //then
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.accessTokenExpiresAt()).isNotNull();
        assertThat(result.refreshTokenExpiresAt()).isNotNull();
    }

    @Test
    void register_happyPath_savesUserInDatabase() {
        //given
        var request = new RegisterRequest("alice@example.com", "hunter22pass");

        //when
        AuthApiUtils.OK.register(request, restTemplate);

        //then
        var user = userRepository.findByEmail("alice@example.com");
        assertThat(user).isPresent();
        assertThat(user.get().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void register_emailNormalisedToLowerCase() {
        //given
        var request = new RegisterRequest("Alice@Example.COM", "hunter22pass");

        //when
        AuthApiUtils.OK.register(request, restTemplate);

        //then
        var user = userRepository.findByEmail("alice@example.com");
        assertThat(user).isPresent();
    }

    @Test
    void register_duplicateEmail_returns409() {
        //given
        AuthApiUtils.OK.register(new RegisterRequest("carol@example.com", "hunter22pass"), restTemplate);

        //when
        var response = AuthApiUtils.Error.register(
                new RegisterRequest("carol@example.com", "hunter22pass"), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getTitle()).isEqualTo("Conflict");
    }

    @Test
    void register_weakPassword_returns400() {
        //when
        var response = AuthApiUtils.Error.register(
                new RegisterRequest("bob@example.com", "short"), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
    }

    @Test
    void register_passwordWithoutDigit_returns400() {
        //when
        var response = AuthApiUtils.Error.register(
                new RegisterRequest("bob@example.com", "nolettersonlyletters"), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_passwordWithoutLetter_returns400() {
        //when
        var response = AuthApiUtils.Error.register(
                new RegisterRequest("bob@example.com", "123456789"), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_invalidEmail_returns400() {
        //when
        var response = AuthApiUtils.Error.register(
                new RegisterRequest("not-an-email", "hunter22pass"), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_blankEmail_returns400() {
        //when
        var response = AuthApiUtils.Error.register(
                new RegisterRequest("", "hunter22pass"), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_blankPassword_returns400() {
        //when
        var response = AuthApiUtils.Error.register(
                new RegisterRequest("bob@example.com", ""), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
