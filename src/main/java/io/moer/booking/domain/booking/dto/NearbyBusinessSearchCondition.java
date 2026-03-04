package io.moer.booking.domain.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyBusinessSearchCondition {
    private Double lat;
    private Double lng;
    private Integer radius; // km, default 5
    private String businessType;
    private String keyword;
    private Integer page;
    private Integer size;

    public int getRadiusOrDefault() {
        return (radius == null || radius < 1) ? 5 : Math.min(radius, 50);
    }

    public int getPageOrDefault() {
        return (page == null || page < 1) ? 1 : page;
    }

    public int getSizeOrDefault() {
        return (size == null || size < 1) ? 20 : Math.min(size, 100);
    }

    public int getOffset() {
        return (getPageOrDefault() - 1) * getSizeOrDefault();
    }
}
