package io.moer.booking.domain.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.customer.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class CustomerSegmentItem {
    private Long id;
    private String name;
    private String phone;
    private Integer visitCount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastVisitDate;

    private Integer totalSpent;

    public static CustomerSegmentItem from(Customer customer) {
        return CustomerSegmentItem.builder()
                .id(customer.getId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .visitCount(customer.getVisitCount())
                .lastVisitDate(customer.getLastVisitDate())
                .totalSpent(customer.getTotalSpent())
                .build();
    }
}
