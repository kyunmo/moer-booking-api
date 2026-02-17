package io.moer.booking.domain.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.BusinessType;
import io.moer.booking.domain.service.Service;
import io.moer.booking.domain.staff.Staff;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 고객용 매장 상세 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class PublicBusinessDetailResponse {

    // ========================================
    // 기본 정보
    // ========================================

    private Long id;
    private String slug;
    private String name;
    private BusinessType businessType;
    private String description;
    private String address;
    private String phone;
    private String profileImageUrl;
    private List<String> galleryImages;

    // ========================================
    // 평점/리뷰
    // ========================================

    private Double averageRating;
    private Integer reviewCount;

    // ========================================
    // 영업시간
    // ========================================

    /**
     * 영업시간 (JSONB 그대로)
     * 예: {"mon":{"open":"09:00","close":"20:00"}, ...}
     */
    private Map<String, Object> businessHours;

    // ========================================
    // 태그
    // ========================================

    private List<String> tags;

    // ========================================
    // 서비스 목록
    // ========================================

    private List<PublicServiceItem> services;

    // ========================================
    // 스태프 목록
    // ========================================

    private List<PublicStaffItem> staffs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 서비스 아이템 (고객에게 보여지는 정보만)
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class PublicServiceItem {
        private Long id;
        private String categoryName;
        private String name;
        private String description;
        private Integer price;
        private Integer duration;

        public static PublicServiceItem from(Service service) {
            return PublicServiceItem.builder()
                    .id(service.getId())
                    .categoryName(service.getCategoryName())
                    .name(service.getName())
                    .description(service.getDescription())
                    .price(service.getPrice())
                    .duration(service.getDuration())
                    .build();
        }
    }

    /**
     * 스태프 아이템 (고객에게 보여지는 정보만)
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class PublicStaffItem {
        private Long id;
        private String name;
        private String position;
        private String profileImageUrl;
        private String introduction;
        private String specialty;
        private int portfolioCount;

        public static PublicStaffItem from(Staff staff, int portfolioCount) {
            return PublicStaffItem.builder()
                    .id(staff.getId())
                    .name(staff.getName())
                    .position(staff.getPosition())
                    .profileImageUrl(staff.getProfileImageUrl())
                    .introduction(staff.getIntroduction())
                    .specialty(staff.getSpecialty())
                    .portfolioCount(portfolioCount)
                    .build();
        }
    }

    /**
     * Business + 관련 데이터로 상세 응답 생성
     *
     * @param business       매장 엔티티
     * @param services       활성 서비스 목록
     * @param staffs         활성 스태프 목록
     * @param portfolioCounts 스태프별 포트폴리오 수 (staffId -> count)
     */
    public static PublicBusinessDetailResponse from(
            Business business,
            List<Service> services,
            List<Staff> staffs,
            Map<Long, Integer> portfolioCounts) {

        // 태그 파싱
        List<String> tagList = null;
        if (business.getTags() != null && !business.getTags().isBlank()) {
            tagList = Arrays.stream(business.getTags().split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toList());
        }

        // 서비스 목록 변환 (활성 서비스만)
        List<PublicServiceItem> serviceItems = null;
        if (services != null && !services.isEmpty()) {
            serviceItems = services.stream()
                    .filter(Service::isActive)
                    .map(PublicServiceItem::from)
                    .collect(Collectors.toList());
        }

        // 스태프 목록 변환
        List<PublicStaffItem> staffItems = null;
        if (staffs != null && !staffs.isEmpty()) {
            staffItems = staffs.stream()
                    .map(staff -> {
                        int count = portfolioCounts != null
                                ? portfolioCounts.getOrDefault(staff.getId(), 0)
                                : 0;
                        return PublicStaffItem.from(staff, count);
                    })
                    .collect(Collectors.toList());
        }

        return PublicBusinessDetailResponse.builder()
                .id(business.getId())
                .slug(business.getSlug())
                .name(business.getName())
                .businessType(business.getBusinessType())
                .description(business.getDescription())
                .address(business.getAddress())
                .phone(business.getPhone())
                .profileImageUrl(business.getProfileImageUrl())
                .galleryImages(business.getGalleryImages())
                .averageRating(business.getAverageRating())
                .reviewCount(business.getReviewCount() != null ? business.getReviewCount() : 0)
                .businessHours(business.getBusinessHours())
                .tags(tagList)
                .services(serviceItems)
                .staffs(staffItems)
                .createdAt(business.getCreatedAt())
                .build();
    }
}
