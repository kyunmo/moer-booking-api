package io.moer.booking.domain.business;

import lombok.Getter;

/**
 * 결제 주기
 */
@Getter
public enum BillingCycle {
    MONTHLY("월간"),
    YEARLY("연간");

    private final String description;

    BillingCycle(String description) {
        this.description = description;
    }

    public boolean isYearly() {
        return this == YEARLY;
    }

    public boolean isMonthly() {
        return this == MONTHLY;
    }
}
