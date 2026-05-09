package com.smarturl.hub.link;

import com.smarturl.hub.link.config.LinkProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestContainersConfig {

    public static final String TEST_QUEUE = "test.click.events.queue";

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:17.2"))
                .withDatabaseName("links_db")
                .withUsername("test")
                .withPassword("test");
    }

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitContainer() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.0.5-management"));
    }

    @Bean
    Queue testClickEventsQueue() {
        return QueueBuilder.nonDurable(TEST_QUEUE).autoDelete().build();
    }

    @Bean
    Binding testClickEventsBinding(Queue testClickEventsQueue, TopicExchange clickEventsExchange,
                                   LinkProperties properties) {
        return BindingBuilder.bind(testClickEventsQueue)
                .to(clickEventsExchange)
                .with(properties.amqp().clickEventsRoutingKey());
    }
}
