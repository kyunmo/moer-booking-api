package io.moer.booking.domain.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.BusinessType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 고객용 매장 목록 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class PublicBusinessListResponse {

    private Long id;
    private String slug;
    private String name;
    private BusinessType businessType;
    private String address;
    private String phone;
    private String profileImageUrl;
    private Double averageRating;
    private Integer reviewCount;

    /**
     * 오늘 영업시간 (예: "10:00 - 20:00")
     */
    private String todayHours;

    /**
     * 현재 영업 중 여부
     */
    private boolean isOpen;

    /**
     * 태그 목록
     */
    private List<String> tags;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Business 엔티티에서 목록 응답 DTO 변환
     */
    public static PublicBusinessListResponse from(Business business) {
        Map<String, Object> businessHours = business.getBusinessHours();
        String todayHours = extractTodayHours(businessHours);
        boolean isOpen = checkIsOpen(businessHours);

        List<String> tagList = null;
        if (business.getTags() != null && !business.getTags().isBlank()) {
            tagList = Arrays.stream(business.getTags().split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toList());
        }

        return PublicBusinessListResponse.builder()
                .id(business.getId())
                .slug(business.getSlug())
                .name(business.getName())
                .businessType(business.getBusinessType())
                .address(business.getAddress())
                .phone(business.getPhone())
                .profileImageUrl(business.getProfileImageUrl())
                .averageRating(business.getAverageRating())
                .reviewCount(business.getReviewCount() != null ? business.getReviewCount() : 0)
                .todayHours(todayHours)
                .isOpen(isOpen)
                .tags(tagList)
                .createdAt(business.getCreatedAt())
                .build();
    }

    /**
     * 오늘 요일에 해당하는 영업시간 추출
     * businessHours 형식: {"mon":{"open":"09:00","close":"20:00"}, ...}
     */
    @SuppressWarnings("unchecked")
    private static String extractTodayHours(Map<String, Object> businessHours) {
        if (businessHours == null || businessHours.isEmpty()) {
            return null;
        }

        String todayKey = getTodayKey();
        Object todayData = businessHours.get(todayKey);

        if (todayData == null) {
            return "휴무";
        }

        if (todayData instanceof Map) {
            Map<String, Object> hours = (Map<String, Object>) todayData;
            String open = (String) hours.get("open");
            String close = (String) hours.get("close");

            if (open != null && close != null) {
                return open + " - " + close;
            }
        }

        return null;
    }

    /**
     * 현재 영업 중인지 확인
     */
    @SuppressWarnings("unchecked")
    private static boolean checkIsOpen(Map<String, Object> businessHours) {
        if (businessHours == null || businessHours.isEmpty()) {
            return false;
        }

        String todayKey = getTodayKey();
        Object todayData = businessHours.get(todayKey);

        if (todayData == null) {
            return false;
        }

        if (todayData instanceof Map) {
            Map<String, Object> hours = (Map<String, Object>) todayData;
            String open = (String) hours.get("open");
            String close = (String) hours.get("close");

            if (open != null && close != null) {
                try {
                    LocalTime openTime = LocalTime.parse(open);
                    LocalTime closeTime = LocalTime.parse(close);
                    LocalTime now = LocalTime.now();

                    return !now.isBefore(openTime) && now.isBefore(closeTime);
                } catch (Exception e) {
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * 오늘 요일 키 반환 (mon, tue, wed, thu, fri, sat, sun)
     */
    private static String getTodayKey() {
        DayOfWeek dayOfWeek = LocalDateTime.now().getDayOfWeek();
        return dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toLowerCase();
    }
}
