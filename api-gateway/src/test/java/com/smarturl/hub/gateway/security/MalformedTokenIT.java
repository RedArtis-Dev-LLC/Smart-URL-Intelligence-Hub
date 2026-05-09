package com.smarturl.hub.gateway.security;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.gateway.AbstractIntegrationTest;
import com.smarturl.hub.gateway.security.utils.GatewayApiUtils;
import com.smarturl.hub.gateway.security.utils.JwtTestUtils;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class MalformedTokenIT extends AbstractIntegrationTest {

    @Test
    void linksRoute_garbageToken_returns401() {
        //when
        var response = GatewayApiUtils.getProblem(
                GatewayApiUtils.LINKS_PROBE_PATH, "this-is-not-a-jwt", restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wireMockServer.findAll(getRequestedFor(urlEqualTo(GatewayApiUtils.LINKS_PROBE_PATH))))
                .isEmpty();
    }

    @Test
    void linksRoute_wrongIssuer_returns401() {
        //given
        var token = JwtTestUtils.issueWithIssuer(
                UUID.randomUUID(), "evil@example.com", "evil-issuer", testRsaKeyPair.privateKey());

        //when
        var response = GatewayApiUtils.getProblem(GatewayApiUtils.LINKS_PROBE_PATH, token, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wireMockServer.findAll(getRequestedFor(urlEqualTo(GatewayApiUtils.LINKS_PROBE_PATH))))
                .isEmpty();
    }

    @Test
    void linksRoute_signedWithUnknownKey_returns401() throws Exception {
        //given a token signed by a key the gateway does not trust
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var foreignPrivateKey = (RSAPrivateKey) generator.generateKeyPair().getPrivate();
        var token = JwtTestUtils.issueValidToken(UUID.randomUUID(), "stranger@example.com", foreignPrivateKey);

        //when
        var response = GatewayApiUtils.getProblem(GatewayApiUtils.LINKS_PROBE_PATH, token, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wireMockServer.findAll(getRequestedFor(urlEqualTo(GatewayApiUtils.LINKS_PROBE_PATH))))
                .isEmpty();
    }
}
