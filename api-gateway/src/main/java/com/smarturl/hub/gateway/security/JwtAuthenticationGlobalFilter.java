package com.smarturl.hub.gateway.security;

import com.smarturl.hub.gateway.security.JwtVerificationService.ParsedToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

    private static final String REVOKED_KEY_PREFIX = "auth:revoked:";
    private static final String PUBLIC_METADATA_KEY = "public";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtVerificationService jwtVerificationService;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ProblemDetailWriter problemDetailWriter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (isPublicRoute(exchange)) {
            return chain.filter(stripIdentityHeaders(exchange));
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return unauthorized(exchange, "Missing or malformed Authorization header");
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return unauthorized(exchange, "Missing or malformed Authorization header");
        }

        ParsedToken parsed;
        try {
            parsed = jwtVerificationService.verify(token);
        } catch (TokenInvalidException ex) {
            log.debug("JWT verification failed: {}", ex.getMessage());
            return unauthorized(exchange, ex.getMessage());
        }

        return redisTemplate.hasKey(REVOKED_KEY_PREFIX + parsed.jti())
                .defaultIfEmpty(Boolean.FALSE)
                .flatMap(revoked -> {
                    if (Boolean.TRUE.equals(revoked)) {
                        return unauthorized(exchange, "Token has been revoked");
                    }
                    return chain.filter(injectIdentityHeaders(exchange, parsed));
                });
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private static boolean isPublicRoute(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return false;
        }
        return Boolean.TRUE.equals(route.getMetadata().get(PUBLIC_METADATA_KEY));
    }

    private static ServerWebExchange stripIdentityHeaders(ServerWebExchange exchange) {
        return exchange.mutate()
                .request(builder -> builder.headers(h -> {
                    h.remove(GatewayHeaders.USER_ID);
                    h.remove(GatewayHeaders.USER_EMAIL);
                    h.remove(GatewayHeaders.USER_ROLES);
                }))
                .build();
    }

    private static ServerWebExchange injectIdentityHeaders(ServerWebExchange exchange, ParsedToken parsed) {
        return exchange.mutate()
                .request(builder -> builder
                        .headers(h -> {
                            h.remove(GatewayHeaders.USER_ID);
                            h.remove(GatewayHeaders.USER_EMAIL);
                            h.remove(GatewayHeaders.USER_ROLES);
                        })
                        .header(GatewayHeaders.USER_ID, parsed.userId().toString())
                        .header(GatewayHeaders.USER_EMAIL, parsed.email() == null ? "" : parsed.email())
                        .header(GatewayHeaders.USER_ROLES, String.join(",", parsed.roles())))
                .build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String detail) {
        return problemDetailWriter.write(exchange, HttpStatus.UNAUTHORIZED, detail);
    }
}
