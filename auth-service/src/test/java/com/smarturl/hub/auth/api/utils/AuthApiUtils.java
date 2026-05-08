package com.smarturl.hub.auth.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.auth.api.dto.AuthResponse;
import com.smarturl.hub.auth.api.dto.LoginRequest;
import com.smarturl.hub.auth.api.dto.RefreshRequest;
import com.smarturl.hub.auth.api.dto.RegisterRequest;
import com.smarturl.hub.auth.api.dto.UserResponse;
import lombok.experimental.UtilityClass;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@UtilityClass
public class AuthApiUtils {

    public static final String REGISTER_URL = "/auth/register";
    public static final String LOGIN_URL = "/auth/login";
    public static final String REFRESH_URL = "/auth/refresh";
    public static final String LOGOUT_URL = "/auth/logout";
    public static final String ME_URL = "/auth/me";
    public static final String PUBLIC_KEY_URL = "/auth/public-key";

    @UtilityClass
    public static final class OK {

        public static AuthResponse register(RegisterRequest request, TestRestTemplate restTemplate) {
            var response = restTemplate.postForEntity(REGISTER_URL, request, AuthResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            return response.getBody();
        }

        public static AuthResponse login(LoginRequest request, TestRestTemplate restTemplate) {
            var response = restTemplate.postForEntity(LOGIN_URL, request, AuthResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            return response.getBody();
        }

        public static AuthResponse refresh(RefreshRequest request, TestRestTemplate restTemplate) {
            var response = restTemplate.postForEntity(REFRESH_URL, request, AuthResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            return response.getBody();
        }

        public static void logout(String accessToken, TestRestTemplate restTemplate) {
            var headers = bearerHeaders(accessToken);
            var response = restTemplate.exchange(
                    LOGOUT_URL, HttpMethod.POST, new HttpEntity<>(headers), Void.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        public static UserResponse me(String accessToken, TestRestTemplate restTemplate) {
            var headers = bearerHeaders(accessToken);
            var response = restTemplate.exchange(
                    ME_URL, HttpMethod.GET, new HttpEntity<>(headers), UserResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            return response.getBody();
        }

        public static String publicKey(TestRestTemplate restTemplate) {
            var response = restTemplate.getForEntity(PUBLIC_KEY_URL, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            return response.getBody();
        }
    }

    @UtilityClass
    public static final class Error {

        public static ResponseEntity<ProblemDetail> register(RegisterRequest request, TestRestTemplate restTemplate) {
            return restTemplate.postForEntity(REGISTER_URL, request, ProblemDetail.class);
        }

        public static ResponseEntity<ProblemDetail> login(LoginRequest request, TestRestTemplate restTemplate) {
            return restTemplate.postForEntity(LOGIN_URL, request, ProblemDetail.class);
        }

        public static ResponseEntity<ProblemDetail> refresh(RefreshRequest request, TestRestTemplate restTemplate) {
            return restTemplate.postForEntity(REFRESH_URL, request, ProblemDetail.class);
        }

        public static ResponseEntity<ProblemDetail> logout(String accessToken, TestRestTemplate restTemplate) {
            var headers = bearerHeaders(accessToken);
            return restTemplate.exchange(
                    LOGOUT_URL, HttpMethod.POST, new HttpEntity<>(headers), ProblemDetail.class);
        }

        public static ResponseEntity<ProblemDetail> logoutNoAuth(TestRestTemplate restTemplate) {
            return restTemplate.postForEntity(LOGOUT_URL, null, ProblemDetail.class);
        }

        public static ResponseEntity<ProblemDetail> me(String accessToken, TestRestTemplate restTemplate) {
            var headers = bearerHeaders(accessToken);
            return restTemplate.exchange(
                    ME_URL, HttpMethod.GET, new HttpEntity<>(headers), ProblemDetail.class);
        }

        public static ResponseEntity<ProblemDetail> meNoAuth(TestRestTemplate restTemplate) {
            return restTemplate.getForEntity(ME_URL, ProblemDetail.class);
        }
    }

    private static HttpHeaders bearerHeaders(String accessToken) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
