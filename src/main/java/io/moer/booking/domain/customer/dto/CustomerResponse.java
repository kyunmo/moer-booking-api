package io.moer.booking.domain.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.customer.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 고객 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class CustomerResponse {

    private Long id;
    private Long businessId;

    private String name;
    private String phone;
    private String email;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private String gender;

    private Integer visitCount;
    private Integer totalSpent;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastVisitDate;

    /**
     * 태그 목록
     * DB의 콤마 구분 문자열을 List로 변환
     */
    private List<String> tags;

    /**
     * 메모
     */
    private String memo;

    /**
     * 고객 타입 자동 판별
     */
    private Boolean isVip;
    private Boolean isNew;
    private Boolean isRegular;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static CustomerResponse from(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .businessId(customer.getBusinessId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .birthDate(customer.getBirthDate())
                .gender(customer.getGender())
                .visitCount(customer.getVisitCount())
                .totalSpent(customer.getTotalSpent())
                .lastVisitDate(customer.getLastVisitDate())
                .tags(customer.getTagList())  // String → List 변환
                .memo(customer.getMemo())
                .isVip(customer.isVip())
                .isNew(customer.isNew())
                .isRegular(customer.isRegular())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}