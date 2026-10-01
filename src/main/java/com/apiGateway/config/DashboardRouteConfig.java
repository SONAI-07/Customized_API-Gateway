package com.apiGateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.URI;

@Configuration
public class DashboardRouteConfig {

    @Bean
    public RouterFunction<ServerResponse> dashboardRoutes() {
        return RouterFunctions.route()
                .GET("/dashboard", req -> ServerResponse.temporaryRedirect(URI.create("/dashboard/")).build())
                .GET("/dashboard/", req -> ServerResponse.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .bodyValue(new ClassPathResource("static/dashboard/index.html")))
                .build();
    }
}
