package com.smarturl.hub.gateway.security;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.gateway.AbstractIntegrationTest;
import com.smarturl.hub.gateway.security.utils.GatewayApiUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PublicRouteIT extends AbstractIntegrationTest {

    @Test
    void redirect_publicRouteWithoutToken_isForwardedWithoutIdentityHeaders() {
        //given a 200 stub so the test does not depend on TestRestTemplate's redirect handling
        wireMockServer.stubFor(get(urlEqualTo(GatewayApiUtils.REDIRECT_PROBE_PATH))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value()).withBody("forwarded")));

        //when
        var response = GatewayApiUtils.getNoAuth(GatewayApiUtils.REDIRECT_PROBE_PATH, restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("forwarded");

        var received = wireMockServer.findAll(getRequestedFor(urlEqualTo(GatewayApiUtils.REDIRECT_PROBE_PATH)));
        assertThat(received).hasSize(1);
        var headers = received.getFirst().getHeaders();
        assertThat(headers.getHeader(GatewayHeaders.USER_ID).isPresent()).isFalse();
        assertThat(headers.getHeader(GatewayHeaders.USER_EMAIL).isPresent()).isFalse();
        assertThat(headers.getHeader(GatewayHeaders.USER_ROLES).isPresent()).isFalse();
    }

    @Test
    void redirect_publicRouteIgnoresProvidedToken_doesNotInjectHeaders() {
        //given
        wireMockServer.stubFor(get(urlEqualTo(GatewayApiUtils.REDIRECT_PROBE_PATH))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value()).withBody("forwarded")));

        //when a malformed token is supplied
        var response = GatewayApiUtils.get(GatewayApiUtils.REDIRECT_PROBE_PATH, "garbage-token", restTemplate);

        //then the public route still forwards
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var received = wireMockServer.findAll(getRequestedFor(urlEqualTo(GatewayApiUtils.REDIRECT_PROBE_PATH)));
        assertThat(received).hasSize(1);
        assertThat(received.getFirst().getHeaders().getHeader(GatewayHeaders.USER_ID).isPresent()).isFalse();
    }
}
