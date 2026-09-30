package com.apiGateway.security;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;
    private final List<String> publicPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtService jwtService,
                                   @Value("${gateway.auth.public-paths}") List<String> publicPaths) {
        this.jwtService = jwtService;
        this.publicPaths = publicPaths;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. Public paths (login, health) skip auth entirely
        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        // 2. Extract Bearer token
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing Bearer token");
        }

        // 3. Validate signature + expiry
        Optional<Claims> maybeClaims = jwtService.validate(header.substring(7));
        if (maybeClaims.isEmpty()) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        // 4. TRUST BOUNDARY: stamp identity headers for downstream services
        Claims claims = maybeClaims.get();
        ServerWebExchange mutated = exchange.mutate().request(r -> r.headers(h -> {
            h.remove("X-User-Id");
            h.remove("X-User-Role");
            h.set("X-User-Id", claims.getSubject());
            h.set("X-User-Role", claims.get("role", String.class));
        })).build();

        return chain.filter(mutated);
    }

    private boolean isPublic(String path) {
        return publicPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        byte[] bytes = ("{\"error\":\"UNAUTHORIZED\",\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0; // after rate limiting (-1), before routing (10000+)
    }
}