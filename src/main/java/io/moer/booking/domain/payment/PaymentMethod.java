package io.moer.booking.domain.payment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 결제 수단
 */
@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    CARD("카드"),
    BANK_TRANSFER("계좌이체"),
    VIRTUAL_ACCOUNT("가상계좌"),
    MOBILE("모바일결제");

    private final String description;
}
