package com.msa.gateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Fallback controller for circuit breaker responses.
 *
 * When a downstream service is unavailable and the circuit breaker trips,
 * requests are redirected here instead of failing with a connection error.
 * This provides a graceful degradation experience for API consumers,
 * returning a structured error response rather than a raw 503.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/users")
    public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "message", "User service is currently unavailable. Please try again later.",
                        "timestamp", LocalDateTime.now().toString()
                )));
    }
}
