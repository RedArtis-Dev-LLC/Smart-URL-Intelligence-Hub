package com.smarturl.hub.analytics.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.analytics.api.dto.LinkSummaryResponse;
import com.smarturl.hub.analytics.api.dto.LinkTimeseriesResponse;
import com.smarturl.hub.common.security.GatewayHeaders;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

@UtilityClass
public class AnalyticsApiUtils {

    public static final String SUMMARY_URL_TEMPLATE = "/analytics/links/{linkId}/summary";
    public static final String TIMESERIES_URL_TEMPLATE = "/analytics/links/{linkId}/timeseries";

    @UtilityClass
    public static final class OK {

        public static LinkSummaryResponse summary(UUID linkId, UUID userId, TestRestTemplate restTemplate) {
            var response = restTemplate.exchange(
                    URI.create(SUMMARY_URL_TEMPLATE.replace("{linkId}", linkId.toString())),
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(userId)),
                    LinkSummaryResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            return response.getBody();
        }

        public static LinkTimeseriesResponse timeseries(UUID linkId, UUID userId,
                                                       LocalDate from, LocalDate to,
                                                       TestRestTemplate restTemplate) {
            URI uri = buildTimeseriesUri(linkId, from, to);
            var response = restTemplate.exchange(uri,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(userId)),
                    LinkTimeseriesResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            return response.getBody();
        }
    }

    @UtilityClass
    public static final class Error {

        public static ResponseEntity<ProblemDetail> summary(UUID linkId, HttpHeaders headers,
                                                            TestRestTemplate restTemplate) {
            return restTemplate.exchange(
                    URI.create(SUMMARY_URL_TEMPLATE.replace("{linkId}", linkId.toString())),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    ProblemDetail.class);
        }

        public static ResponseEntity<ProblemDetail> timeseries(UUID linkId, UUID userId,
                                                               LocalDate from, LocalDate to,
                                                               TestRestTemplate restTemplate) {
            URI uri = buildTimeseriesUri(linkId, from, to);
            return restTemplate.exchange(uri,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(userId)),
                    ProblemDetail.class);
        }
    }

    private static URI buildTimeseriesUri(UUID linkId, LocalDate from, LocalDate to) {
        var builder = UriComponentsBuilder.fromPath(TIMESERIES_URL_TEMPLATE.replace("{linkId}", linkId.toString()));
        if (from != null) builder.queryParam("from", from);
        if (to != null) builder.queryParam("to", to);
        return builder.build().toUri();
    }

    public static HttpHeaders authHeaders(UUID userId) {
        var headers = new HttpHeaders();
        headers.add(GatewayHeaders.USER_ID, userId.toString());
        headers.add(GatewayHeaders.USER_EMAIL, "test@example.com");
        headers.add(GatewayHeaders.USER_ROLES, "ROLE_USER");
        return headers;
    }
}
