package com.smarturl.hub.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestContainersConfig {

    private static final String REDIS_PASSWORD = "test-redis-pass";

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:17.2"))
                .withDatabaseName("auth_db")
                .withUsername("test")
                .withPassword("test");
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

    @Bean
    DynamicPropertyRegistrar rsaKeyProperties() {
        try {
            var gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            Path dir = Files.createTempDirectory("auth-it-keys");
            Path privateKeyPath = writePem(dir.resolve("private.pem"), "PRIVATE KEY", pair.getPrivate().getEncoded());
            Path publicKeyPath = writePem(dir.resolve("public.pem"), "PUBLIC KEY", pair.getPublic().getEncoded());

            return registry -> {
                registry.add("auth.jwt.private-key-path", () -> "file:" + privateKeyPath);
                registry.add("auth.jwt.public-key-path", () -> "file:" + publicKeyPath);
            };
        } catch (Exception e) {
            throw new IllegalStateException("Failed to bootstrap test RSA key pair", e);
        }
    }

    private static Path writePem(Path target, String label, byte[] der) throws IOException {
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
        String pem = "-----BEGIN " + label + "-----\n" + base64 + "\n-----END " + label + "-----\n";
        Files.writeString(target, pem);
        return target;
    }
}
