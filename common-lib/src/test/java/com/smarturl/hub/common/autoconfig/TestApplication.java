package com.smarturl.hub.common.autoconfig;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class TestApplication {

    @RestController
    public static class StubController {

        @GetMapping("/whoami")
        public String whoami() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth == null ? "anonymous" : Objects.requireNonNull(auth.getPrincipal()).toString();
        }

        @PostMapping("/echo")
        public StubResponse echo(@Valid @RequestBody StubRequest request) {
            return new StubResponse(request.email());
        }

        @GetMapping("/forbidden")
        public String forbidden() {
            throw new AccessDeniedException("nope");
        }

        @GetMapping("/boom")
        public String boom() {
            throw new IllegalStateException("internal blow-up — must not leak");
        }
    }

    public record StubRequest(
            @NotBlank @Size(min = 3, max = 50) String email,
            @NotBlank String password) {}

    public record StubResponse(String email) {}
}
