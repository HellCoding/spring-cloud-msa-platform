package com.msa.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Discovery Server - central service registry for the MSA platform.
 *
 * All microservices register themselves here on startup, enabling service-to-service
 * communication via logical service names instead of hardcoded host:port values.
 * This decoupling is essential for dynamic scaling and deployment in cloud environments.
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
