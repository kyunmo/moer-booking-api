package io.moer.booking.common.controller;

import io.moer.booking.common.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;

@RestController
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse(
                "OK",
                "moer-booking API is running",
                LocalDateTime.now()
        );
    }

    @GetMapping("/health/db")
    public HealthResponse healthDb() {
        try (Connection conn = dataSource.getConnection()) {
            return new HealthResponse(
                    "OK",
                    "Database connection successful",
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            return new HealthResponse(
                    "ERROR",
                    "Database connection failed: " + e.getMessage(),
                    LocalDateTime.now()
            );
        }
    }
}