package com.apiGateway.registry.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {
    private String serviceName;   // e.g. ORDER-SERVICE
    private String instanceID;    // e.g. ORDER-SERVICE-192.168.1.5-8081
    private String host;          // e.g. 192.168.1.5
    private Long port;            // e.g. 8081
    private int weight;           // reserved for future weighted routing
}