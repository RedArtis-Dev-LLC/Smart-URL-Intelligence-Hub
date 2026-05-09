package com.smarturl.hub.gateway.security;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.gateway.AbstractIntegrationTest;
import com.smarturl.hub.gateway.security.utils.GatewayApiUtils;
import com.smarturl.hub.gateway.security.utils.JwtTestUtils;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ExpiredTokenIT extends AbstractIntegrationTest {

    @Test
    void linksRoute_expiredToken_returns401() {
        //given
        var token = JwtTestUtils.issueExpiredToken(
                UUID.randomUUID(), "alice@example.com", testRsaKeyPair.privateKey());

        //when
        var response = GatewayApiUtils.getProblem(GatewayApiUtils.LINKS_PROBE_PATH, token, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).contains("expired");
        assertThat(wireMockServer.findAll(getRequestedFor(urlEqualTo(GatewayApiUtils.LINKS_PROBE_PATH))))
                .isEmpty();
    }
}
