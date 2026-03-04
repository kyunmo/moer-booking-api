package io.moer.booking.domain.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CustomerMergeResponse {

    private Long primaryCustomerId;
    private List<Long> mergedCustomerIds;
    private int mergedReservationCount;
    private int mergedNoteCount;
    private CustomerResponse mergedCustomer;
}
