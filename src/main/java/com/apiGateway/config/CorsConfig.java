package com.apiGateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Hackathon Friendly: Allow all origins (localhost:3000, Netlify, Vercel, etc.)
        config.setAllowedOriginPatterns(Arrays.asList("*"));

        // Allow all standard HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Allow all headers (crucial for Authorization and X-API-Key)
        config.setAllowedHeaders(Arrays.asList("*"));

        // Allow cookies/auth headers (Set to false if using AllowedOrigins("*") strictly,
        // but AllowedOriginPatterns("*") allows true)
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour (saves network roundtrips)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // Apply to everything

        return new CorsWebFilter(source);
    }
}