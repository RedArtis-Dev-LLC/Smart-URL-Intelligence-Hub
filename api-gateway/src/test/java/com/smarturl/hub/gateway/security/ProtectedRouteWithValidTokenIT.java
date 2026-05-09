package com.smarturl.hub.gateway.security;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.gateway.AbstractIntegrationTest;
import com.smarturl.hub.gateway.security.utils.GatewayApiUtils;
import com.smarturl.hub.gateway.security.utils.JwtTestUtils;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ProtectedRouteWithValidTokenIT extends AbstractIntegrationTest {

    @Test
    void linksRoute_validToken_forwardsWithIdentityHeaders() {
        //given
        var userId = UUID.randomUUID();
        var email = "alice@example.com";
        var token = JwtTestUtils.issueValidToken(userId, email, testRsaKeyPair.privateKey());
        wireMockServer.stubFor(get(urlEqualTo(GatewayApiUtils.LINKS_PROBE_PATH))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value()).withBody("{\"ok\":true}")));

        //when
        var response = GatewayApiUtils.get(GatewayApiUtils.LINKS_PROBE_PATH, token, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var received = wireMockServer.findAll(getRequestedFor(urlEqualTo(GatewayApiUtils.LINKS_PROBE_PATH)));
        assertThat(received).hasSize(1);
        var headers = received.getFirst().getHeaders();
        assertThat(headers.getHeader(GatewayHeaders.USER_ID).firstValue()).isEqualTo(userId.toString());
        assertThat(headers.getHeader(GatewayHeaders.USER_EMAIL).firstValue()).isEqualTo(email);
        assertThat(headers.getHeader(GatewayHeaders.USER_ROLES).firstValue()).contains("ROLE_USER");
    }

    @Test
    void analyticsRoute_validToken_forwardsWithIdentityHeaders() {
        //given
        var userId = UUID.randomUUID();
        var token = JwtTestUtils.issueValidToken(userId, "bob@example.com", testRsaKeyPair.privateKey());
        wireMockServer.stubFor(get(urlEqualTo(GatewayApiUtils.ANALYTICS_PROBE_PATH))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())));

        //when
        var response = GatewayApiUtils.get(GatewayApiUtils.ANALYTICS_PROBE_PATH, token, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var received = wireMockServer.findAll(getRequestedFor(urlEqualTo(GatewayApiUtils.ANALYTICS_PROBE_PATH)));
        assertThat(received).hasSize(1);
        assertThat(received.getFirst().getHeaders().getHeader(GatewayHeaders.USER_ID).firstValue())
                .isEqualTo(userId.toString());
    }

    @Test
    void webhooksRoute_validToken_forwardsWithIdentityHeaders() {
        //given
        var userId = UUID.randomUUID();
        var token = JwtTestUtils.issueValidToken(userId, "carol@example.com", testRsaKeyPair.privateKey());
        wireMockServer.stubFor(get(urlEqualTo(GatewayApiUtils.WEBHOOKS_PROBE_PATH))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())));

        //when
        var response = GatewayApiUtils.get(GatewayApiUtils.WEBHOOKS_PROBE_PATH, token, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var received = wireMockServer.findAll(getRequestedFor(urlEqualTo(GatewayApiUtils.WEBHOOKS_PROBE_PATH)));
        assertThat(received).hasSize(1);
        assertThat(received.getFirst().getHeaders().getHeader(GatewayHeaders.USER_ID).firstValue())
                .isEqualTo(userId.toString());
    }
}
