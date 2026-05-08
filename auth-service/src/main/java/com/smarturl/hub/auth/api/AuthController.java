package com.smarturl.hub.auth.api;

import com.smarturl.hub.auth.api.dto.AuthResponse;
import com.smarturl.hub.auth.api.dto.LoginRequest;
import com.smarturl.hub.auth.api.dto.RefreshRequest;
import com.smarturl.hub.auth.api.dto.RegisterRequest;
import com.smarturl.hub.auth.api.dto.UserResponse;
import com.smarturl.hub.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final String publicKeyPem;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.logout(authorization);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return authService.currentUser(authorization);
    }

    @GetMapping(value = "/public-key", produces = MediaType.TEXT_PLAIN_VALUE)
    public String publicKey() {
        return publicKeyPem;
    }
}
