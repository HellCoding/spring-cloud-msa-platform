package com.msa.user.client;

/**
 * DTO representing the auth-service's user data.
 * Using a Java record for immutability and concise syntax.
 */
public record AuthUserResponse(
        Long id,
        String email,
        String name,
        String role
) {}
