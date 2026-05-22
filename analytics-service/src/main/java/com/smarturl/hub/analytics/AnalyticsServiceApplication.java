package com.smarturl.hub.analytics;

import com.smarturl.hub.analytics.config.AnalyticsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableConfigurationProperties(AnalyticsProperties.class)
@EnableFeignClients(basePackages = "com.smarturl.hub.analytics.feign")
public class AnalyticsServiceApplication {
    static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
