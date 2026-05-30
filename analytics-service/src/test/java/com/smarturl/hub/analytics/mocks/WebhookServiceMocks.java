package com.smarturl.hub.analytics.mocks;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.smarturl.hub.analytics.feign.webhook.WebhookThreshold;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

@UtilityClass
public class WebhookServiceMocks {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String THRESHOLDS_URL = "/internal/webhooks/thresholds";

    @SneakyThrows
    public static void mockThresholds_200(WireMockServer wireMockServer, UUID linkId, List<WebhookThreshold> body) {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(THRESHOLDS_URL))
                .withQueryParam("linkId", WireMock.equalTo(linkId.toString()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(OBJECT_MAPPER.writeValueAsBytes(body))));
    }

    @SneakyThrows
    public static void mockThresholds_emptyForAny(WireMockServer wireMockServer) {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(THRESHOLDS_URL))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(OBJECT_MAPPER.writeValueAsBytes(List.<WebhookThreshold>of()))));
    }

    public static void mockThresholds_500(WireMockServer wireMockServer) {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(THRESHOLDS_URL))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }
}
