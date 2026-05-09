package com.smarturl.hub.link;

import com.smarturl.hub.link.domain.LinkRepository;
import com.smarturl.hub.link.outbox.OutboxRepository;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected LinkRepository linkRepository;

    @Autowired
    protected OutboxRepository outboxRepository;

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected RabbitAdmin rabbitAdmin;

    protected final UUID userId = UUID.randomUUID();

    @BeforeAll
    void disableRedirectFollowing() {
        restTemplate.getRestTemplate().setRequestFactory(new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        });
    }

    @BeforeEach
    void cleanUp() {
        outboxRepository.deleteAll();
        linkRepository.deleteAll();
    }
}
