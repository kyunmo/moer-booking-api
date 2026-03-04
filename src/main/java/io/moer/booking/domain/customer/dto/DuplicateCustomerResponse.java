package io.moer.booking.domain.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DuplicateCustomerResponse {

    private String phone;
    private int count;
    private List<CustomerResponse> customers;
}
