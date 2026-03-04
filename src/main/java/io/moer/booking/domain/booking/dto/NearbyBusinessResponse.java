package io.moer.booking.domain.booking.dto;

import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.BusinessType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NearbyBusinessResponse {
    private Long id;
    private String businessName;
    private BusinessType businessType;
    private String address;
    private Double lat;
    private Double lng;
    private Double distance; // km
    private Double rating;
    private Integer reviewCount;
    private String imageUrl;
    private String slug;

    public static NearbyBusinessResponse from(Business business, Double distance) {
        return NearbyBusinessResponse.builder()
                .id(business.getId())
                .businessName(business.getName())
                .businessType(business.getBusinessType())
                .address(business.getAddress())
                .lat(business.getLatitude())
                .lng(business.getLongitude())
                .distance(distance != null ? Math.round(distance * 10) / 10.0 : null)
                .rating(business.getAverageRating())
                .reviewCount(business.getReviewCount() != null ? business.getReviewCount() : 0)
                .imageUrl(business.getProfileImageUrl())
                .slug(business.getSlug())
                .build();
    }
}
