package com.apiGateway.registry.controller;

import com.apiGateway.registry.model.RegistrationRequest;
import com.apiGateway.registry.model.ServiceInstance;
import com.apiGateway.registry.service.RegisterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/registry")
public class RegistryController {

    private static final Logger logger = LoggerFactory.getLogger(RegistryController.class);
    private final RegisterService registerService;

    public RegistryController(RegisterService registerService) {
        this.registerService = registerService;
    }

    /**
     * POST /registry/register
     * Called by a microservice once at startup to join the gateway's registry.
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<Map<String, String>>> register(@RequestBody RegistrationRequest request) {
        ServiceInstance instance = new ServiceInstance(
                request.getServiceName(),
                request.getInstanceID(),
                request.getHost(),
                null,                  // timeoutReference -> assigned by RegisterService
                request.getPort(),
                request.getWeight(),
                new AtomicInteger(),                     // activeConnections starts at 0
                null                   // lastHeartBeatTime -> stamped by RegisterService
        );

        logger.info("Registration request from instance: {}", request.getInstanceID());

        return registerService.register(instance)
                .then(Mono.just(ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(Map.of(
                                "status", "REGISTERED",
                                "serviceName", request.getServiceName(),
                                "instanceID", request.getInstanceID(),
                                "heartbeatDeadlineSeconds", "90"
                        ))));
    }

    /**
     * POST /registry/heartbeat?serviceName=X&instanceID=Y
     * Called periodically (e.g. every 30s) by microservices to prove liveness.
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, String>> heartbeat(@RequestParam String serviceName,
                                                         @RequestParam String instanceID) {
        boolean renewed = registerService.renewHeartbeat(serviceName, instanceID);
        if (renewed) {
            return ResponseEntity.ok(Map.of("status", "HEARTBEAT_ACCEPTED"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", "UNKNOWN_INSTANCE", "action", "RE_REGISTER"));
    }

    /**
     * DELETE /registry/deregister?serviceName=X&instanceID=Y
     * Called on graceful shutdown (e.g. from a @PreDestroy hook) so traffic
     * stops flowing to a dying instance immediately, instead of waiting 90s.
     */
    @DeleteMapping("/deregister")
    public ResponseEntity<Map<String, String>> deregister(@RequestParam String serviceName,
                                                          @RequestParam String instanceID) {
        registerService.deleteInstance(serviceName, instanceID);
        return ResponseEntity.ok(Map.of("status", "DEREGISTERED", "instanceID", instanceID));
    }

    /**
     * GET /registry/services
     * Observability endpoint: dumps the entire live registry as JSON.
     */
    @GetMapping("/services")
    public ResponseEntity<?> listServices() {
        return ResponseEntity.ok(registerService.serviceFinder);
    }
}