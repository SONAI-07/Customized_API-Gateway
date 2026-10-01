package com.apiGateway.observability;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(1)
public class AcceptedRequestsFilter implements WebFilter {
    private final GatewayMetrics metrics;
    public AcceptedRequestsFilter(GatewayMetrics metrics) { this.metrics = metrics; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (exchange.getRequest().getMethod() != HttpMethod.OPTIONS) {
            metrics.recordAccepted();
        }
        return chain.filter(exchange);
    }
}