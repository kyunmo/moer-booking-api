package io.moer.booking.domain.business.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class OnboardingStatusResponse {
    private Boolean completed;
    private Boolean skipped;
    private List<OnboardingStep> steps;
    private String currentStep;
    private Integer completedSteps;
    private Integer totalSteps;
}
