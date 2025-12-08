package com.msa.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Declarative Feign client for auth-service communication.
 *
 * The name "auth-service" is resolved via Eureka to the actual service instances.
 * FallbackFactory (not Fallback) is used because it provides access to the
 * exception that triggered the fallback, enabling better error logging.
 */
@FeignClient(name = "auth-service", fallbackFactory = AuthServiceClientFallback.class)
public interface AuthServiceClient {

    @GetMapping("/auth/users/{userId}")
    AuthUserResponse getUserById(@PathVariable("userId") Long userId);
}
