package io.moer.booking.domain.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerHistory {
    private Long id;
    private Long businessId;
    private Long customerId;
    private Long staffId;
    private Long reservationId;
    private LocalDate visitDate;
    private List<Map<String, Object>> services;  // JSONB
    private Integer totalPrice;
    private String paymentMethod;  // CARD, CASH, TRANSFER
    private Map<String, Object> details;  // JSONB
    private String beforeImageUrl;
    private String afterImageUrl;
    private LocalDateTime createdAt;
}