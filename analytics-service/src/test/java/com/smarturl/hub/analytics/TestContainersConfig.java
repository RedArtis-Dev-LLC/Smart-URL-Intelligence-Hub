package com.smarturl.hub.analytics;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestContainersConfig {

    private static final String REDIS_PASSWORD = "test-redis-pass";

    @Bean
    @ServiceConnection
    MongoDBContainer mongoContainer() {
        return new MongoDBContainer(DockerImageName.parse("mongo:8.0"));
    }

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitContainer() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.0.5-management"));
    }

    @Bean
    GenericContainer<?> redisContainer() {
        var container = new GenericContainer<>(DockerImageName.parse("redis:8.0.1-alpine"))
                .withCommand("redis-server", "--requirepass", REDIS_PASSWORD)
                .withExposedPorts(6379)
                .waitingFor(Wait.forListeningPort());
        container.start();
        return container;
    }

    @Bean
    DynamicPropertyRegistrar redisProperties(GenericContainer<?> redisContainer) {
        return registry -> {
            registry.add("spring.data.redis.host", redisContainer::getHost);
            registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
            registry.add("spring.data.redis.password", () -> REDIS_PASSWORD);
        };
    }

    @Bean(destroyMethod = "stop")
    WireMockServer wireMockServer() {
        var server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        return server;
    }

    @Bean
    DynamicPropertyRegistrar wireMockProperties(WireMockServer wireMockServer) {
        return registry -> {
            registry.add("analytics.geo.base-url", wireMockServer::baseUrl);
            registry.add("analytics.webhook-service.base-url", wireMockServer::baseUrl);
        };
    }
}
