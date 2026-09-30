package com.apiGateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class RegistryProtectionFilter implements GlobalFilter, Ordered {

    private final byte[] registryTokenBytes;

    public RegistryProtectionFilter(@Value("${gateway.registry.token:}") String registryToken) {
        this.registryTokenBytes = registryToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
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

    @Override
    public int getOrder() {
        return -2; // the very first guard
    }
}