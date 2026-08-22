package com.nowrongdoor.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Application health check endpoint.
 * Independent of mock service connectivity — this confirms
 * the Spring Boot application itself is running.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "no-wrong-door-backend"
        ));
    }
}
