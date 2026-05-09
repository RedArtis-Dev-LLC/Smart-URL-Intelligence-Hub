package com.smarturl.hub.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@ConditionalOnClass({SecurityFilterChain.class, UsernamePasswordAuthenticationToken.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableWebSecurity
public class HeaderAuthSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GatewayAuthFilter gatewayAuthFilter() {
        return new GatewayAuthFilter();
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationEntryPoint.class)
    public AuthenticationEntryPoint problemDetailAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new ProblemDetailAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            GatewayAuthFilter gatewayAuthFilter,
            AuthenticationEntryPoint authenticationEntryPoint) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(eh -> eh.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(gatewayAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
