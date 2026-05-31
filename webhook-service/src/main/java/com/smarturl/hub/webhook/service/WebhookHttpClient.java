package com.smarturl.hub.webhook.service;

import com.smarturl.hub.webhook.amqp.event.ThresholdReachedEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class WebhookHttpClient {

    private final RestTemplate restTemplate;

    @CircuitBreaker(name = "webhook-delivery")
    public int deliver(String targetUrl, ThresholdReachedEvent event) {
        var payload = new WebhookPayload(
                event.eventId(),
                event.linkId(),
                event.shortCode(),
                event.originalUrl(),
                event.threshold(),
                event.currentCount(),
                event.reachedAt());

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(targetUrl, request, String.class);
        return response.getStatusCode().value();
    }
}
