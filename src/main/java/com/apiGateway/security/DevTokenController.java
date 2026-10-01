package com.apiGateway.security;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Profile("dev")
@RestController
@RequestMapping("/auth")
public class DevTokenController {

    private final JwtService jwtService;

    public DevTokenController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/dev-token")
    public Map<String, String> mintToken(@RequestParam(defaultValue = "user-1") String userId,
                                         @RequestParam(defaultValue = "USER") String role) {
        return Map.of("token", jwtService.generateToken(userId, role));
    }
}