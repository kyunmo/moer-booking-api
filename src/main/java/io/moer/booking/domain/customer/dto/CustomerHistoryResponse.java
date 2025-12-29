package io.moer.booking.domain.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.customer.CustomerHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class CustomerHistoryResponse {

    private Long id;
    private Long customerId;
    private Long businessId;
    private Long reservationId;
    private Long staffId;

    // 직원 이름 (조인 필요 - 추후)
    private String staffName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate visitDate;

    private List<Map<String, Object>> services;
    private Integer totalPrice;

    private Map<String, Object> details;
    private String beforeImageUrl;
    private String afterImageUrl;

    private String adminMemo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public static CustomerHistoryResponse from(CustomerHistory history) {
        return CustomerHistoryResponse.builder()
                .id(history.getId())
                .customerId(history.getCustomerId())
                .businessId(history.getBusinessId())
                .reservationId(history.getReservationId())
                .staffId(history.getStaffId())
                .visitDate(history.getVisitDate())
                .services(history.getServices())
                .totalPrice(history.getTotalPrice())
                .details(history.getDetails())
                .beforeImageUrl(history.getBeforeImageUrl())
                .afterImageUrl(history.getAfterImageUrl())
                .adminMemo(history.getAdminMemo())
                .createdAt(history.getCreatedAt())
                .build();
    }
}