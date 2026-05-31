package com.smarturl.hub.webhook.mocks;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LinkServiceMocks {

    public static void mockGetLink_200(WireMockServer server, UUID linkId, UUID userId) {
        server.stubFor(get(urlPathEqualTo("/links/" + linkId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"%s","userId":"%s"}
                                """.formatted(linkId, userId))));
    }

    public static void mockGetLink_404(WireMockServer server, UUID linkId) {
        server.stubFor(get(urlPathEqualTo("/links/" + linkId))
                .willReturn(aResponse().withStatus(404)));
    }
}
