package com.smarturl.hub.gateway.security.utils;

import lombok.experimental.UtilityClass;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@UtilityClass
public class GatewayApiUtils {

    public static final String LINKS_PROBE_PATH = "/links/probe";
    public static final String ANALYTICS_PROBE_PATH = "/analytics/links/probe/summary";
    public static final String WEBHOOKS_PROBE_PATH = "/webhooks";
    public static final String REDIRECT_PROBE_PATH = "/abc123";

    public static ResponseEntity<String> get(String path, String accessToken, TestRestTemplate restTemplate) {
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(bearerHeaders(accessToken)), String.class);
    }

    public static ResponseEntity<String> getWithHeaders(
            String path, HttpHeaders headers, TestRestTemplate restTemplate) {
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    public static ResponseEntity<String> getNoAuth(String path, TestRestTemplate restTemplate) {
        return restTemplate.getForEntity(path, String.class);
    }

    public static ResponseEntity<ProblemDetail> getProblem(
            String path, String accessToken, TestRestTemplate restTemplate) {
        return restTemplate.exchange(
                path, HttpMethod.GET, new HttpEntity<>(bearerHeaders(accessToken)), ProblemDetail.class);
    }

    public static ResponseEntity<ProblemDetail> getProblemNoAuth(String path, TestRestTemplate restTemplate) {
        return restTemplate.getForEntity(path, ProblemDetail.class);
    }

    private static HttpHeaders bearerHeaders(String accessToken) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
