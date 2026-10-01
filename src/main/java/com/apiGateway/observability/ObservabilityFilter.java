package com.apiGateway.observability;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(-100) // before every guard
public class ObservabilityFilter implements WebFilter {
    private final GatewayMetrics metrics;
    public ObservabilityFilter(GatewayMetrics metrics) { this.metrics = metrics; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        boolean isPreflight = exchange.getRequest().getMethod() == HttpMethod.OPTIONS;
        boolean isInternal = path.startsWith("/actuator") || path.startsWith("/gateway/observability");
        if (!isPreflight && !isInternal) {
            metrics.recordReceived();
        }
        return chain.filter(exchange);
    }
}