package com.smarturl.hub.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshTokenServiceTest {

    @Test
    void hash_isStableAndAvalanches() {
        String a = RefreshTokenService.hash("abc123");
        String b = RefreshTokenService.hash("abc123");
        String c = RefreshTokenService.hash("abc124");

        assertThat(a).hasSize(64).isEqualTo(b);
        assertThat(c).isNotEqualTo(a);
    }
}
