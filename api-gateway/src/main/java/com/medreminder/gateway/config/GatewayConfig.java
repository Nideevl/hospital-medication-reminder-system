package com.medreminder.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("patients", r -> r
                .path("/api/patients/**")
                .filters(f -> f.stripPrefix(1))
                .uri("http://localhost:8081"))
            .route("schedules", r -> r
                .path("/api/schedules/**")
                .filters(f -> f.stripPrefix(1))
                .uri("http://localhost:8082"))
            .route("calls", r -> r
                .path("/api/calls/**")
                .filters(f -> f.stripPrefix(1))
                .uri("http://localhost:8083"))
            .route("escalations", r -> r
                .path("/api/escalations/**")
                .filters(f -> f.stripPrefix(1))
                .uri("http://localhost:8084"))
            .route("reports", r -> r
                .path("/api/reports/**")
                .filters(f -> f.stripPrefix(1))
                .uri("http://localhost:8085"))
            .route("notifications", r -> r
                .path("/api/notifications/**")
                .filters(f -> f.stripPrefix(1))
                .uri("http://localhost:8086"))
            .route("audit", r -> r
                .path("/api/audit/**")
                .filters(f -> f.stripPrefix(1))
                .uri("http://localhost:8087"))
            .build();
    }
}
