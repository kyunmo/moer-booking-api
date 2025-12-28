package io.moer.booking.domain.business;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessType {
    BEAUTY_SHOP("미용실"),
    PILATES("필라테스/요가"),
    CAFE("스터디카페/공방"),
    CLINIC("병원/한의원"),
    ACADEMY("학원"),
    PET_SALON("애견미용"),
    OTHER("기타");

    private final String description;
}