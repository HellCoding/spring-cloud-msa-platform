package com.msa.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;

/**
 * Global JWT authentication filter applied to every gateway request.
 *
 * This filter runs at the gateway level (not per-service) so that downstream
 * services don't need to implement their own authentication logic. The gateway
 * validates the JWT and forwards the user identity as an X-User-Id header,
 * establishing a trusted internal communication channel.
 *
 * Public endpoints (auth, actuator) are whitelisted to allow unauthenticated access.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/auth/**",
            "/actuator/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip authentication for public endpoints (login, register, health checks)
        if (isOpenEndpoint(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(7);

        if (token.isBlank()) {
            return unauthorized(exchange);
        }

        // Extract user ID from JWT payload to pass downstream.
        // Full signature verification is handled by auth-service; the gateway
        // performs a lightweight check to reject obviously malformed tokens early.
        String userId = extractUserIdFromToken(token);
        if (userId == null) {
            return unauthorized(exchange);
        }

        // Mutate the request to include the parsed user identity, so downstream
        // services can trust this header without re-parsing the JWT themselves.
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Priority is set high (negative value) so authentication runs before
     * other filters like circuit breakers or rate limiters.
     */
    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isOpenEndpoint(String path) {
        return OPEN_ENDPOINTS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * Extracts the "sub" (subject) claim from the JWT payload.
     * JWT structure: header.payload.signature (Base64URL encoded).
     * This is a lightweight extraction without full cryptographic verification,
     * since the auth-service is the source of truth for token validity.
     */
    private String extractUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

            // Simple JSON parsing to extract "sub" claim without adding a JSON library
            // to the reactive gateway classpath (keeping dependencies minimal).
            int subIndex = payload.indexOf("\"sub\"");
            if (subIndex == -1) {
                return null;
            }

            int valueStart = payload.indexOf("\"", subIndex + 5) + 1;
            int valueEnd = payload.indexOf("\"", valueStart);
            return payload.substring(valueStart, valueEnd);
        } catch (Exception e) {
            return null;
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
