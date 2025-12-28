package io.moer.booking.common.controller;

import io.moer.booking.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "OK");
        data.put("message", "moer-booking API is running");
        data.put("timestamp", LocalDateTime.now());

        return ApiResponse.success(data);
    }

    @GetMapping("/health/db")
    public ApiResponse<Map<String, Object>> healthDb() {
        Map<String, Object> data = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            data.put("status", "OK");
            data.put("message", "Database connection successful");
            data.put("database", conn.getMetaData().getDatabaseProductName());
            data.put("timestamp", LocalDateTime.now());

            return ApiResponse.success(data);
        } catch (Exception e) {
            data.put("status", "ERROR");
            data.put("message", "Database connection failed");
            data.put("error", e.getMessage());

            return ApiResponse.error("DB001", "데이터베이스 연결 실패", data);
        }
    }
}