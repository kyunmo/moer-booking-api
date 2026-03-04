package io.moer.booking.domain.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CustomerTagResponse {

    private List<String> tags;
    private int count;

    public static CustomerTagResponse of(List<String> tags) {
        return CustomerTagResponse.builder()
                .tags(tags)
                .count(tags.size())
                .build();
    }
}
