package com.smarturl.hub.webhook.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.webhook.api.dto.CreateWebhookRequest;
import com.smarturl.hub.webhook.api.dto.UpdateWebhookRequest;
import com.smarturl.hub.webhook.api.dto.WebhookConfigResponse;
import com.smarturl.hub.webhook.api.dto.WebhookThresholdResponse;
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
public class WebhookApiUtils {

    public static final String WEBHOOKS_URL = "/webhooks";
    public static final String INTERNAL_THRESHOLDS_URL = "/internal/webhooks/thresholds";

    @UtilityClass
    public static final class OK {

        public static WebhookConfigResponse create(CreateWebhookRequest request, UUID userId, TestRestTemplate restTemplate) {
            var response = restTemplate.exchange(
                    WEBHOOKS_URL, HttpMethod.POST, new HttpEntity<>(request, userHeaders(userId)), WebhookConfigResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            return response.getBody();
        }

        public static WebhookConfigResponse[] list(UUID userId, TestRestTemplate restTemplate) {
            var response = restTemplate.exchange(
                    WEBHOOKS_URL, HttpMethod.GET, new HttpEntity<>(userHeaders(userId)), WebhookConfigResponse[].class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            return response.getBody();
        }

        public static WebhookConfigResponse get(UUID id, UUID userId, TestRestTemplate restTemplate) {
            var response = restTemplate.exchange(
                    WEBHOOKS_URL + "/" + id, HttpMethod.GET, new HttpEntity<>(userHeaders(userId)), WebhookConfigResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            return response.getBody();
        }

        public static WebhookConfigResponse update(UUID id, UpdateWebhookRequest request, UUID userId, TestRestTemplate restTemplate) {
            var response = restTemplate.exchange(
                    WEBHOOKS_URL + "/" + id, HttpMethod.PUT, new HttpEntity<>(request, userHeaders(userId)), WebhookConfigResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            return response.getBody();
        }

        public static void delete(UUID id, UUID userId, TestRestTemplate restTemplate) {
            var response = restTemplate.exchange(
                    WEBHOOKS_URL + "/" + id, HttpMethod.DELETE, new HttpEntity<>(userHeaders(userId)), Void.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        public static WebhookThresholdResponse[] thresholds(UUID linkId, TestRestTemplate restTemplate) {
            var response = restTemplate.exchange(
                    INTERNAL_THRESHOLDS_URL + "?linkId=" + linkId, HttpMethod.GET, null, WebhookThresholdResponse[].class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            return response.getBody();
        }
    }

    @UtilityClass
    public static final class Error {

        public static ResponseEntity<ProblemDetail> create(CreateWebhookRequest request, UUID userId, TestRestTemplate restTemplate) {
            return restTemplate.exchange(
                    WEBHOOKS_URL, HttpMethod.POST, new HttpEntity<>(request, userHeaders(userId)), ProblemDetail.class);
        }

        public static ResponseEntity<ProblemDetail> get(UUID id, UUID userId, TestRestTemplate restTemplate) {
            return restTemplate.exchange(
                    WEBHOOKS_URL + "/" + id, HttpMethod.GET, new HttpEntity<>(userHeaders(userId)), ProblemDetail.class);
        }

        public static ResponseEntity<ProblemDetail> getNoAuth(UUID id, TestRestTemplate restTemplate) {
            return restTemplate.exchange(
                    WEBHOOKS_URL + "/" + id, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), ProblemDetail.class);
        }

        public static ResponseEntity<ProblemDetail> update(UUID id, UpdateWebhookRequest request, UUID userId, TestRestTemplate restTemplate) {
            return restTemplate.exchange(
                    WEBHOOKS_URL + "/" + id, HttpMethod.PUT, new HttpEntity<>(request, userHeaders(userId)), ProblemDetail.class);
        }

        public static ResponseEntity<ProblemDetail> delete(UUID id, UUID userId, TestRestTemplate restTemplate) {
            return restTemplate.exchange(
                    WEBHOOKS_URL + "/" + id, HttpMethod.DELETE, new HttpEntity<>(userHeaders(userId)), ProblemDetail.class);
        }

        public static ResponseEntity<ProblemDetail> listNoAuth(TestRestTemplate restTemplate) {
            return restTemplate.exchange(
                    WEBHOOKS_URL, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), ProblemDetail.class);
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
