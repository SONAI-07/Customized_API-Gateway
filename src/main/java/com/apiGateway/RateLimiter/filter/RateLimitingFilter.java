package com.apiGateway.RateLimiter.filter;

import com.apiGateway.RateLimiter.model.TokenBucket;
import com.apiGateway.RateLimiter.service.TokenBucketRegistry;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetAddress;

@Component
@Order(-1) // Runs SECOND
public class RateLimitingFilter implements WebFilter {

    private final TokenBucketRegistry registry;

    public RateLimitingFilter(TokenBucketRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }
        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        String identifier;

        if (apiKey != null && !apiKey.isBlank()) {
            identifier = apiKey;
        } else {
            InetAddress remoteAddress = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress()
                    : null;
            identifier = remoteAddress != null ? remoteAddress.getHostAddress() : "unknown-client";
        }

        TokenBucket bucket = registry.getBucket(identifier);

        if (bucket.tryConsume()) {
            return chain.filter(exchange);
        } else {
            long remaining = bucket.getAvailableTokens();
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", "60");
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));
            exchange.getResponse().getHeaders().add("Retry-After", "60");
            return exchange.getResponse().setComplete();
        }
    }
}

