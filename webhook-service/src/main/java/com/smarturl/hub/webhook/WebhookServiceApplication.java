package com.smarturl.hub.webhook;

import com.smarturl.hub.webhook.config.WebhookProperties;
import io.github.resilience4j.springboot3.verifier.autoconfigure.SpringBoot3VerifierAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(exclude = SpringBoot3VerifierAutoConfiguration.class)
@EnableConfigurationProperties(WebhookProperties.class)
@EnableFeignClients(basePackages = "com.smarturl.hub.webhook.feign")
public class WebhookServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebhookServiceApplication.class, args);
    }
}
