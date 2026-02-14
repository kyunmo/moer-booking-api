package io.moer.booking.domain.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class OnboardingStep {
    private String step;
    private String label;
    private Boolean completed;
}
