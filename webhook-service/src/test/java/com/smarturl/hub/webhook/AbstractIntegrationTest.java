package com.smarturl.hub.webhook;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.smarturl.hub.webhook.config.WebhookProperties;
import com.smarturl.hub.webhook.domain.WebhookConfigRepository;
import com.smarturl.hub.webhook.domain.WebhookDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
    protected WebhookConfigRepository configRepository;

    @Autowired
    protected WebhookDeliveryRepository deliveryRepository;

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected WireMockServer wireMockServer;

    @Autowired
    protected WebhookProperties webhookProperties;

    @BeforeEach
    void cleanUp() {
        deliveryRepository.deleteAll();
        configRepository.deleteAll();
        wireMockServer.resetAll();
    }
}
