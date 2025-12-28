package io.moer.booking.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class HealthResponse {
    private String status;
    private String message;
    private LocalDateTime timestamp;
}