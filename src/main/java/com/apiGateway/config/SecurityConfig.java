package com.apiGateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                // Disable CSRF since this is a stateless API Gateway
                .csrf(csrf -> csrf.disable())
                // Disable the default Basic Auth and Form Login that are causing the 401s
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                // Allow all requests to pass through to the Gateway Filters
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                );

        return http.build();
    }
}