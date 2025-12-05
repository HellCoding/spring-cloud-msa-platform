package com.msa.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Authentication Service - handles user registration, login, and JWT issuance.
 *
 * This service owns the auth-bounded context: user credentials, password hashing,
 * and token generation. Other services never access passwords directly; they rely
 * on JWT tokens issued by this service for identity verification.
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
