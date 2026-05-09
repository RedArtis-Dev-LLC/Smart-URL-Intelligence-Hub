package com.smarturl.hub.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.smarturl.hub.gateway.TestContainersConfig.TestRsaKeyPair;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import="
        }
)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@AutoConfigureTestRestTemplate
public abstract class AbstractIntegrationTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected WireMockServer wireMockServer;

    @Autowired
    protected ReactiveStringRedisTemplate redis;

    @Autowired
    protected TestRsaKeyPair testRsaKeyPair;

    @BeforeEach
    void cleanUp() {
        wireMockServer.resetAll();
        redis.getConnectionFactory().getReactiveConnection().serverCommands().flushAll().block();
    }
}
