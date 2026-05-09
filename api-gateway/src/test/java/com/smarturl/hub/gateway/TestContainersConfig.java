package com.smarturl.hub.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestContainersConfig {

    private static final String REDIS_PASSWORD = "test-redis-pass";

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
        return registry -> registry.add("test.downstream.uri", wireMockServer::baseUrl);
    }

    @Bean
    TestRsaKeyPair testRsaKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            Path dir = Files.createTempDirectory("gateway-it-keys");
            Path publicKeyPath = writePem(dir.resolve("public.pem"), "PUBLIC KEY", pair.getPublic().getEncoded());
            return new TestRsaKeyPair(
                    (RSAPrivateKey) pair.getPrivate(),
                    (RSAPublicKey) pair.getPublic(),
                    publicKeyPath);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to bootstrap test RSA key pair", e);
        }
    }

    @Bean
    DynamicPropertyRegistrar rsaProperties(TestRsaKeyPair keyPair) {
        return registry -> registry.add(
                "gateway.jwt.public-key-path", () -> "file:" + keyPair.publicKeyPath());
    }

    public record TestRsaKeyPair(RSAPrivateKey privateKey, RSAPublicKey publicKey, Path publicKeyPath) {}

    private static Path writePem(Path target, String label, byte[] der) throws IOException {
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
        String pem = "-----BEGIN " + label + "-----\n" + base64 + "\n-----END " + label + "-----\n";
        Files.writeString(target, pem);
        return target;
    }
}
