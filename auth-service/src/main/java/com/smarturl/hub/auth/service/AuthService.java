package com.smarturl.hub.auth.service;

import com.smarturl.hub.auth.api.dto.AuthResponse;
import com.smarturl.hub.auth.api.dto.UserResponse;
import com.smarturl.hub.auth.config.JwtProperties;
import com.smarturl.hub.auth.domain.User;
import com.smarturl.hub.auth.domain.UserRepository;
import com.smarturl.hub.auth.error.EmailAlreadyExistsException;
import com.smarturl.hub.auth.error.InvalidCredentialsException;
import com.smarturl.hub.auth.error.TokenInvalidException;
import com.smarturl.hub.auth.service.JwtService.IssuedAccessToken;
import com.smarturl.hub.auth.service.JwtService.ParsedAccessToken;
import com.smarturl.hub.auth.service.RefreshTokenService.IssuedRefreshToken;
import com.smarturl.hub.auth.service.RefreshTokenService.RotationResult;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenRevocationService revocationService;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Transactional
    public AuthResponse register(String email, String rawPassword) {
        String normalisedEmail = email.toLowerCase();
        if (userRepository.existsByEmail(normalisedEmail)) {
            throw new EmailAlreadyExistsException();
        }
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(normalisedEmail)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .createdAt(clock.instant())
                .build();
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException _) {
            throw new EmailAlreadyExistsException();
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RotationResult rotation = refreshTokenService.rotate(refreshToken);
        User user = userRepository.findById(rotation.userId())
                .orElseThrow(() -> new TokenInvalidException("User no longer exists"));
        IssuedAccessToken access = jwtService.issueAccessToken(user.getId(), user.getEmail());
        return AuthResponse.of(access, rotation.next());
    }

    public void logout(String bearerHeader) {
        ParsedAccessToken parsed = jwtService.parseAndVerify(extractBearer(bearerHeader));
        if (revocationService.isRevoked(parsed.jti())) {
            return;
        }
        revocationService.revoke(parsed.jti(), parsed.expiresAt());
    }

    public UserResponse currentUser(String bearerHeader) {
        ParsedAccessToken parsed = jwtService.parseAndVerify(extractBearer(bearerHeader));
        if (revocationService.isRevoked(parsed.jti())) {
            throw new TokenInvalidException("Token has been revoked");
        }
        return new UserResponse(parsed.userId(), parsed.email());
    }

    private AuthResponse issueTokens(User user) {
        IssuedAccessToken access = jwtService.issueAccessToken(user.getId(), user.getEmail());
        IssuedRefreshToken refresh = refreshTokenService.issue(user.getId());
        return AuthResponse.of(access, refresh);
    }

    private static String extractBearer(String header) {
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new TokenInvalidException("Missing or malformed Authorization header");
        }
        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            throw new TokenInvalidException("Missing or malformed Authorization header");
        }
        return token;
    }
}
