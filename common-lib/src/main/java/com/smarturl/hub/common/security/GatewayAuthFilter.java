package com.smarturl.hub.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class GatewayAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String userId = request.getHeader(GatewayHeaders.USER_ID);
        if (userId == null || userId.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        UUID parsedId;
        try {
            parsedId = UUID.fromString(userId);
        } catch (IllegalArgumentException _) {
            chain.doFilter(request, response);
            return;
        }

        String email = request.getHeader(GatewayHeaders.USER_EMAIL);
        List<String> roles = parseRoles(request.getHeader(GatewayHeaders.USER_ROLES));

        AuthenticatedUser principal = new AuthenticatedUser(parsedId, email, roles);
        var authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .map(a -> (org.springframework.security.core.GrantedAuthority) a)
                .toList();

        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        chain.doFilter(request, response);
    }

    private static List<String> parseRoles(String header) {
        if (header == null || header.isBlank()) {
            return List.of();
        }
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
