package com.smarturl.hub.gateway.security;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.gateway.AbstractIntegrationTest;
import com.smarturl.hub.gateway.security.utils.GatewayApiUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class MissingTokenIT extends AbstractIntegrationTest {

    @Test
    void linksRoute_noAuthorizationHeader_returns401AndDoesNotForward() {
        //when
        var response = GatewayApiUtils.getProblemNoAuth(GatewayApiUtils.LINKS_PROBE_PATH, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Unauthorized");
        assertThat(response.getBody().getDetail()).contains("Authorization header");
        assertThat(wireMockServer.findAll(getRequestedFor(urlEqualTo(GatewayApiUtils.LINKS_PROBE_PATH))))
                .isEmpty();
    }

    @Test
    void linksRoute_emptyBearer_returns401() {
        //when
        var response = GatewayApiUtils.getProblem(GatewayApiUtils.LINKS_PROBE_PATH, "", restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wireMockServer.findAll(getRequestedFor(urlEqualTo(GatewayApiUtils.LINKS_PROBE_PATH))))
                .isEmpty();
    }
}
