package io.moer.booking.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RecentCustomer {
    private Long id;
    private String name;
    private String phone;
    private Integer visitCount;
    private String createdAt;
}