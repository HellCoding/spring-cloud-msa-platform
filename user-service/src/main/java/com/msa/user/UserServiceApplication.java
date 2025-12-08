package com.msa.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * User Service - manages user profiles and orchestrates cross-service data.
 *
 * @EnableFeignClients activates declarative REST client support, allowing
 * this service to call auth-service via interface definitions rather than
 * manual RestTemplate/WebClient code. Feign integrates with Eureka for
 * service discovery and Resilience4j for circuit breaking out of the box.
 */
@SpringBootApplication
@EnableFeignClients
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
