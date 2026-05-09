package com.smarturl.hub.gateway.security;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.gateway.AbstractIntegrationTest;
import com.smarturl.hub.gateway.security.utils.GatewayApiUtils;
import com.smarturl.hub.gateway.security.utils.JwtTestUtils;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RevokedTokenIT extends AbstractIntegrationTest {

    private static final String REVOKED_KEY_PREFIX = "auth:revoked:";

    @Test
    void linksRoute_jtiRevoked_returns401() {
        //given
        var jti = UUID.randomUUID();
        var token = JwtTestUtils.issueValidTokenWithJti(
                UUID.randomUUID(), jti, "alice@example.com", testRsaKeyPair.privateKey());
        redis.opsForValue().set(REVOKED_KEY_PREFIX + jti, "1", Duration.ofMinutes(15)).block();

        //when
        var response = GatewayApiUtils.getProblem(GatewayApiUtils.LINKS_PROBE_PATH, token, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).contains("revoked");
        assertThat(wireMockServer.findAll(getRequestedFor(urlEqualTo(GatewayApiUtils.LINKS_PROBE_PATH))))
                .isEmpty();
    }

    @Test
    void linksRoute_jtiNotRevoked_forwards() {
        //given a fresh token whose jti is NOT in the revocation set
        var token = JwtTestUtils.issueValidToken(
                UUID.randomUUID(), "bob@example.com", testRsaKeyPair.privateKey());
        wireMockServer.stubFor(get(urlEqualTo(GatewayApiUtils.LINKS_PROBE_PATH))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())));

        //when
        var response = GatewayApiUtils.get(GatewayApiUtils.LINKS_PROBE_PATH, token, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(wireMockServer.findAll(getRequestedFor(urlEqualTo(GatewayApiUtils.LINKS_PROBE_PATH))))
                .hasSize(1);
    }
}
