package com.apiGateway.observability;

import com.apiGateway.registry.model.ServiceInstance;
import com.apiGateway.registry.service.RegisterService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GatewayMetrics {

    private final MeterRegistry registry;
    private final Counter receivedRequests;
    private final Counter acceptedRequests;
    private final MultiGauge activeConnections;
    private final MultiGauge instanceCounts;
    private final RegisterService registerService;

    public GatewayMetrics(MeterRegistry registry, RegisterService registerService) {
        this.registry = registry;
        this.registerService = registerService;

        this.receivedRequests = Counter.builder("gateway.requests.received")
                .description("Total requests hitting the gateway edge").register(registry);
        this.acceptedRequests = Counter.builder("gateway.requests.accepted")
                .description("Requests that passed all edge guards").register(registry);

        this.activeConnections = MultiGauge.builder("gateway.connections.active")
                .description("Live connections per service instance").register(registry);
        this.instanceCounts = MultiGauge.builder("gateway.registry.instances")
                .description("Live instances per service").register(registry);
    }

    public void recordReceived()  { receivedRequests.increment(); }
    public void recordAccepted()  { acceptedRequests.increment(); }
    public void recordBlocked(String reason) {
        Counter.builder("gateway.requests.blocked").tag("reason", reason)
                .register(registry).increment();
    }

    @Scheduled(fixedDelay = 5000)
    public void refreshGauges() {
        List<MultiGauge.Row<?>> connRows = new ArrayList<>();
        List<MultiGauge.Row<?>> instRows = new ArrayList<>();

        registerService.serviceFinder.forEach((serviceName, inner) -> {
            instRows.add(MultiGauge.Row.of(Tags.of("service", serviceName), inner.size()));
            for (ServiceInstance inst : inner.values()) {
                connRows.add(MultiGauge.Row.of(
                        Tags.of("service", serviceName, "instance", inst.getInstanceID()),
                        inst.getActiveConnections().get()));
            }
        });

        activeConnections.register(connRows, true);  // true = replace stale rows (evicted instances vanish)
        instanceCounts.register(instRows, true);
    }
}
