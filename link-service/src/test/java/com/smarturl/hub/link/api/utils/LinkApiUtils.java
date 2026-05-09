package com.smarturl.hub.link.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.link.api.dto.CreateLinkRequest;
import com.smarturl.hub.link.api.dto.LinkResponse;
import com.smarturl.hub.link.api.dto.PagedLinksResponse;
import java.util.UUID;
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
public class LinkApiUtils {

    public static final String LINKS_URL = "/links";

    @UtilityClass
    public static final class OK {

        public static LinkResponse create(CreateLinkRequest request, UUID userId, TestRestTemplate restTemplate) {
            var response = restTemplate.exchange(
                    LINKS_URL, HttpMethod.POST, new HttpEntity<>(request, userHeaders(userId)), LinkResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            return response.getBody();
        }

        public static LinkResponse get(UUID id, UUID userId, TestRestTemplate restTemplate) {
            var response = restTemplate.exchange(
                    LINKS_URL + "/" + id, HttpMethod.GET, new HttpEntity<>(userHeaders(userId)), LinkResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            return response.getBody();
        }

        public static PagedLinksResponse list(int page, int size, UUID userId, TestRestTemplate restTemplate) {
            String url = LINKS_URL + "?page=" + page + "&size=" + size;
            var response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(userHeaders(userId)), PagedLinksResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            return response.getBody();
        }

        public static void delete(UUID id, UUID userId, TestRestTemplate restTemplate) {
            var response = restTemplate.exchange(
                    LINKS_URL + "/" + id, HttpMethod.DELETE, new HttpEntity<>(userHeaders(userId)), Void.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    @UtilityClass
    public static final class Error {

        public static ResponseEntity<ProblemDetail> create(
                CreateLinkRequest request, UUID userId, TestRestTemplate restTemplate) {
            return restTemplate.exchange(
                    LINKS_URL, HttpMethod.POST, new HttpEntity<>(request, userHeaders(userId)), ProblemDetail.class);
        }

        public static ResponseEntity<ProblemDetail> get(UUID id, UUID userId, TestRestTemplate restTemplate) {
            return restTemplate.exchange(
                    LINKS_URL + "/" + id, HttpMethod.GET, new HttpEntity<>(userHeaders(userId)), ProblemDetail.class);
        }

        public static ResponseEntity<ProblemDetail> delete(UUID id, UUID userId, TestRestTemplate restTemplate) {
            return restTemplate.exchange(
                    LINKS_URL + "/" + id, HttpMethod.DELETE, new HttpEntity<>(userHeaders(userId)), ProblemDetail.class);
        }

        public static ResponseEntity<ProblemDetail> createNoAuth(CreateLinkRequest request, TestRestTemplate restTemplate) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return restTemplate.exchange(
                    LINKS_URL, HttpMethod.POST, new HttpEntity<>(request, headers), ProblemDetail.class);
        }
    }

    public static HttpHeaders userHeaders(UUID userId) {
        var headers = new HttpHeaders();
        headers.set("X-User-Id", userId.toString());
        headers.set("X-User-Email", "test+" + userId + "@example.com");
        headers.set("X-User-Roles", "ROLE_USER");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
