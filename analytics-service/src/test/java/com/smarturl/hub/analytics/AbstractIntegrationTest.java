package com.smarturl.hub.analytics;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.smarturl.hub.analytics.config.AnalyticsProperties;
import com.smarturl.hub.analytics.domain.ClickEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    protected ClickEventRepository clickEventRepository;

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected StringRedisTemplate redis;

    @Autowired
    protected WireMockServer wireMockServer;

    @Autowired
    protected AnalyticsProperties analyticsProperties;

    @BeforeEach
    void cleanUp() {
        clickEventRepository.deleteAll();
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
        wireMockServer.resetAll();
    }
}
