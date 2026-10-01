package com.apiGateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpMethod;
import com.apiGateway.observability.GatewayMetrics;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@Order(-2) // Runs FIRST
public class RegistryProtectionFilter implements WebFilter {

    private final byte[] registryTokenBytes;
    private GatewayMetrics metrics;

    public RegistryProtectionFilter(@Value("${gateway.registry.token:}") String registryToken, GatewayMetrics metrics) {
        this.registryTokenBytes = registryToken.getBytes(StandardCharsets.UTF_8);
        this.metrics = metrics;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();
        if (!path.startsWith("/registry")) {
            return chain.filter(exchange);
        }

        String supplied = exchange.getRequest().getHeaders().getFirst("X-Registry-Token");
        byte[] suppliedBytes = (supplied == null ? "" : supplied).getBytes(StandardCharsets.UTF_8);

        if (registryTokenBytes.length == 0 ||
                !MessageDigest.isEqual(registryTokenBytes, suppliedBytes)) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }
}