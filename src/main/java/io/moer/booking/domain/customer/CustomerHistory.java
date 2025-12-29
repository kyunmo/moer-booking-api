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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerHistory {

    private Long id;
    private Long customerId;
    private Long businessId;
    private Long reservationId;
    private Long staffId;

    private LocalDate visitDate;
    private List<Map<String, Object>> services;  // [{"id": 1, "name": "여성컷", "price": 30000}]
    private Integer totalPrice;

    // 미용실 특화 정보
    private Map<String, Object> details;  // {"color": "밝은 갈색", "length": "어깨선"}
    private String beforeImageUrl;
    private String afterImageUrl;

    private String adminMemo;

    private LocalDateTime createdAt;
}