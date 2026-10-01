package com.apiGateway.security;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import com.apiGateway.observability.GatewayMetrics;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Order(0) // Runs THIRD
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;
    private final List<String> publicPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final GatewayMetrics metrics;

    public JwtAuthenticationFilter(JwtService jwtService,GatewayMetrics metrics,
                                   @Value("${gateway.auth.public-paths:}") String publicPathsCsv) {
        this.jwtService = jwtService;
        this.metrics = metrics;
        if (publicPathsCsv == null || publicPathsCsv.isBlank()) {
            this.publicPaths = Collections.emptyList();
        } else {
            this.publicPaths = Arrays.stream(publicPathsCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getURI().getPath();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing Bearer token");
        }

        Optional<Claims> maybeClaims = jwtService.validate(header.substring(7));
        if (maybeClaims.isEmpty()) {
            return unauthorized(exchange, "Invalid or expired token");
        }

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
        metrics.recordBlocked("unauthorized");
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}