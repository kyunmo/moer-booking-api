package io.moer.booking.domain.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.customer.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CustomerResponse {

    private Long id;
    private Long businessId;

    private String name;
    private String phone;
    private String email;

    private Integer visitCount;
    private Integer totalSpent;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastVisitDate;

    private List<String> tags;
    private String adminMemo;

    // 고객 타입 자동 판별
    private Boolean isVip;
    private Boolean isNew;
    private Boolean isRegular;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public static CustomerResponse from(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .businessId(customer.getBusinessId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .visitCount(customer.getVisitCount())
                .totalSpent(customer.getTotalSpent())
                .lastVisitDate(customer.getLastVisitDate())
                .tags(customer.getTags())
                .adminMemo(customer.getAdminMemo())
                .isVip(customer.isVip())
                .isNew(customer.isNew())
                .isRegular(customer.isRegular())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}