package com.smarturl.hub.configserver;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigServerIT {

    @LocalServerPort
    int port;

    private RestClient client;

    @BeforeAll
    void setUp() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void linkServiceDefault_returnsServiceSpecificProperties() {
        JsonNode body = client.get().uri("/link-service/default").retrieve().body(JsonNode.class);

        assertThat(body).isNotNull();
        assertThat(body.path("name").asString()).isEqualTo("link-service");
        assertThat(body.path("profiles")).hasSize(1);
        assertThat(body.path("profiles").get(0).asString()).isEqualTo("default");

        JsonNode serviceSource = findSource(body, "link-service.yml");
        assertThat(serviceSource.path("server.port").asInt()).isEqualTo(8082);
        assertThat(serviceSource.path("spring.datasource.url").asString())
                .endsWith("/links_db");
    }

    @Test
    void linkServiceDefault_includesSharedApplicationProperties() {
        JsonNode body = client.get().uri("/link-service/default").retrieve().body(JsonNode.class);

        JsonNode sharedSource = findSource(body, "application.yml");
        assertThat(sharedSource.path("management.tracing.sampling.probability").asDouble())
                .isEqualTo(1.0d);
        assertThat(sharedSource.path("management.zipkin.tracing.endpoint").asString())
                .contains("zipkin");
        assertThat(sharedSource.path("management.endpoints.web.exposure.include").asString())
                .contains("prometheus");
        assertThat(sharedSource.path("spring.rabbitmq.host").asString()).isNotEmpty();
        assertThat(sharedSource.path("spring.data.redis.host").asString()).isNotEmpty();
    }

    @Test
    void unknownService_returnsSharedConfigOnly() {
        JsonNode body = client.get().uri("/no-such-service/default").retrieve().body(JsonNode.class);

        assertThat(body).isNotNull();
        assertThat(body.path("name").asString()).isEqualTo("no-such-service");

        JsonNode propertySources = body.path("propertySources");
        assertThat(propertySources).hasSize(1);
        assertThat(propertySources.get(0).path("name").asString()).contains("application.yml");
    }

    @Test
    void healthEndpoint_isUp() {
        JsonNode body = client.get().uri("/actuator/health").retrieve().body(JsonNode.class);

        assertThat(body).isNotNull();
        assertThat(body.path("status").asString()).isEqualTo("UP");
    }

    private static JsonNode findSource(JsonNode body, String fileSuffix) {
        JsonNode propertySources = body.path("propertySources");
        for (JsonNode ps : propertySources) {
            if (ps.path("name").asString().endsWith(fileSuffix)) {
                return ps.path("source");
            }
        }
        throw new AssertionError("propertySource ending with '" + fileSuffix
                + "' not found; got: " + propertySources);
    }
}
