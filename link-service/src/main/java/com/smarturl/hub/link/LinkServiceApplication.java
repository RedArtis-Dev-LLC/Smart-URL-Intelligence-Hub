package com.smarturl.hub.link;

import com.smarturl.hub.link.config.LinkProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(LinkProperties.class)
public class LinkServiceApplication {
    static void main(String[] args) {
        SpringApplication.run(LinkServiceApplication.class, args);
    }
}
