package com.smarturl.hub.auth.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.auth.AbstractIntegrationTest;
import com.smarturl.hub.auth.api.utils.AuthApiUtils;
import org.junit.jupiter.api.Test;

class PublicKeyIT extends AbstractIntegrationTest {

    @Test
    void publicKey_returnsValidPem() {
        //when
        var body = AuthApiUtils.OK.publicKey(restTemplate);

        //then
        assertThat(body)
                .contains("BEGIN PUBLIC KEY")
                .contains("END PUBLIC KEY");
    }

    @Test
    void publicKey_isAccessibleWithoutAuthentication() {
        //when
        var response = restTemplate.getForEntity(AuthApiUtils.PUBLIC_KEY_URL, String.class);

        //then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
