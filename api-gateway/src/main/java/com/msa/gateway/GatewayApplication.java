package com.msa.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway - single entry point for all client requests.
 *
 * Responsibilities:
 * - Route requests to appropriate microservices via Eureka service discovery
 * - Apply cross-cutting concerns (JWT authentication, rate limiting)
 * - Circuit breaking with Resilience4j to prevent cascade failures
 *
 * Using Spring Cloud Gateway (reactive, Netty-based) rather than Zuul
 * for better performance and native Spring WebFlux integration.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
