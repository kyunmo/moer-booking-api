package io.moer.booking.domain.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CustomerSegmentResponse {
    private String segmentType;
    private int count;
    private List<CustomerSegmentItem> customers;
}
