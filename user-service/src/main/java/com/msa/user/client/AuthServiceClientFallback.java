package com.msa.user.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback factory for auth-service Feign client.
 *
 * When auth-service is unreachable or the circuit breaker trips,
 * this factory provides a graceful degradation by returning placeholder data
 * instead of propagating the failure. This keeps user-service partially
 * functional even when auth-service is down.
 */
@Slf4j
@Component
public class AuthServiceClientFallback implements FallbackFactory<AuthServiceClient> {

    @Override
    public AuthServiceClient create(Throwable cause) {
        log.error("Auth service call failed, activating fallback. Reason: {}", cause.getMessage());

        return userId -> new AuthUserResponse(
                userId,
                "unavailable",
                "Unknown User",
                "USER"
        );
    }
}
