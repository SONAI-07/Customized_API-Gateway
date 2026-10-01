package com.apiGateway.observability;

import com.apiGateway.registry.model.ServiceInstance;
import com.apiGateway.registry.service.RegisterService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/gateway/observability")
public class ObservabilityController {

    private final MeterRegistry registry;
    private final RegisterService registerService;

    public ObservabilityController(MeterRegistry registry, RegisterService registerService) {
        this.registry = registry;
        this.registerService = registerService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalRequests", count("gateway.requests.received"));
        out.put("acceptedRequests", count("gateway.requests.accepted"));
        out.put("blockedRequests", blockedByReason());
        out.put("services", serviceBreakdown());
        return out;
    }

    private double count(String name) {
        return registry.get(name).counters().stream().mapToDouble(Counter::count).sum();
    }

    private Map<String, Double> blockedByReason() {
        Map<String, Double> blocked = new LinkedHashMap<>();
        registry.get("gateway.requests.blocked").counters()
                .forEach(c -> blocked.merge(c.getId().getTag("reason"), c.count(), Double::sum));
        return blocked;
    }

    private List<Map<String, Object>> serviceBreakdown() {
        List<Map<String, Object>> services = new ArrayList<>();
        registerService.serviceFinder.forEach((name, inner) -> {
            Map<String, Object> svc = new LinkedHashMap<>();
            List<Map<String, Object>> instances = new ArrayList<>();
            ServiceInstance busiest = null;

            for (ServiceInstance i : inner.values()) {
                instances.add(Map.of(
                        "instanceID", i.getInstanceID(),
                        "host", String.valueOf(i.getHost()),
                        "port", i.getPort(),
                        "activeConnections", i.getActiveConnections().get()));
                if (busiest == null || i.getActiveConnections().get() > busiest.getActiveConnections().get()) {
                    busiest = i;
                }
            }

            svc.put("serviceName", name);
            svc.put("instanceCount", inner.size());
            svc.put("instances", instances);
            svc.put("busiestInstance", busiest == null ? null : busiest.getInstanceID());
            svc.put("busiestInstanceConnections", busiest == null ? 0 : busiest.getActiveConnections().get());
            services.add(svc);
        });
        return services;
    }
}
