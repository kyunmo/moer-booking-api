package io.moer.booking.domain.customer.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerHistoryCreateRequest {

    @NotNull(message = "고객 ID는 필수입니다")
    private Long customerId;

    private Long reservationId;  // 예약에서 생성되는 경우
    private Long staffId;

    @NotNull(message = "방문 날짜는 필수입니다")
    private LocalDate visitDate;

    @NotEmpty(message = "서비스는 최소 1개 이상이어야 합니다")
    private List<Map<String, Object>> services;  // [{"id": 1, "name": "여성컷", "price": 30000}]

    @NotNull(message = "총 금액은 필수입니다")
    private Integer totalPrice;

    private Map<String, Object> details;  // {"color": "밝은 갈색", "length": "어깨선"}
    private String beforeImageUrl;
    private String afterImageUrl;
    private String adminMemo;
}