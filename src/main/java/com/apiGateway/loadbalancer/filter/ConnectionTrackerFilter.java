package com.apiGateway.loadbalancer.filter;

import com.apiGateway.registry.model.ServiceInstance;
import com.apiGateway.registry.service.RegisterService;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectionTrackerFilter implements GlobalFilter, Ordered {

    private final RegisterService registerService;

    public ConnectionTrackerFilter(RegisterService registerService) {
        this.registerService = registerService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. Grab the instance chosen by your CustomLoadBalancer
        Response<org.springframework.cloud.client.ServiceInstance> response =
                exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR);

        if (response == null || !response.hasServer()) {
            return chain.filter(exchange); // No routing happening, pass through
        }

        org.springframework.cloud.client.ServiceInstance chosenLbInstance = response.getServer();
        String serviceName = chosenLbInstance.getServiceId();
        String instanceId = chosenLbInstance.getInstanceId();

        // 2. Find our custom ServiceInstance in the Registry
        ConcurrentHashMap<String, ServiceInstance> innerMap = registerService.serviceFinder.get(serviceName);
        ServiceInstance customInstance = (innerMap != null) ? innerMap.get(instanceId) : null;

        if (customInstance != null) {
            // INCREMENT before sending the request downstream
            customInstance.getActiveConnections().incrementAndGet();
        }

        // 3. Route the request to the microservice
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // DECREMENT when the response comes back (or if it fails)
            if (customInstance != null) {
                customInstance.getActiveConnections().decrementAndGet();
            }
        }));
    }

    @Override
    public int getOrder() {
        // LoadBalancerClientFilter runs at 10150. We run immediately after it.
        return 10151;
    }
}