package com.smarturl.hub.analytics.mocks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.smarturl.hub.analytics.feign.geo.GeoLookupResponse;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

@UtilityClass
public class GeoApiMocks {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String LOOKUP_URL_PATTERN = "/json/.*";

    public static void mockLookup_200(WireMockServer wireMockServer) {
        mockLookup_200(wireMockServer, _ -> {});
    }

    @SneakyThrows
    public static void mockLookup_200(WireMockServer wireMockServer, Consumer<Builder> modifier) {
        var builder = new Builder("success", "Germany", "Berlin", null);
        modifier.accept(builder);
        var body = new GeoLookupResponse(builder.status, builder.country, builder.city, builder.message);
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathMatching(LOOKUP_URL_PATTERN))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(OBJECT_MAPPER.writeValueAsBytes(body))));
    }

    @SneakyThrows
    public static void mockLookup_failure(WireMockServer wireMockServer) {
        var body = new GeoLookupResponse("fail", null, null, "reserved range");
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathMatching(LOOKUP_URL_PATTERN))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(OBJECT_MAPPER.writeValueAsBytes(body))));
    }

    public static void mockLookup_500(WireMockServer wireMockServer) {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathMatching(LOOKUP_URL_PATTERN))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }

    public static class Builder {
        public String status;
        public String country;
        public String city;
        public String message;

        Builder(String status, String country, String city, String message) {
            this.status = status;
            this.country = country;
            this.city = city;
            this.message = message;
        }
    }
}
