package com.smarturl.hub.webhook.api;

import com.smarturl.hub.common.security.AuthenticatedUser;
import com.smarturl.hub.webhook.api.dto.CreateWebhookRequest;
import com.smarturl.hub.webhook.api.dto.UpdateWebhookRequest;
import com.smarturl.hub.webhook.api.dto.WebhookConfigResponse;
import com.smarturl.hub.webhook.service.WebhookConfigService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookConfigService service;

    @PostMapping
    public ResponseEntity<WebhookConfigResponse> create(
            @Valid @RequestBody CreateWebhookRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(user.userId(), request));
    }

    @GetMapping
    public List<WebhookConfigResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.listByUser(user.userId());
    }

    @GetMapping("/{id}")
    public WebhookConfigResponse get(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return service.get(id, user.userId());
    }

    @PutMapping("/{id}")
    public WebhookConfigResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWebhookRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return service.update(id, user.userId(), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        service.delete(id, user.userId());
        return ResponseEntity.noContent().build();
    }
}
