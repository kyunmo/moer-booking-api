package io.moer.booking.domain.booking.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.domain.booking.dto.NearbyBusinessSearchCondition;
import io.moer.booking.domain.booking.dto.PlatformStatsResponse;
import io.moer.booking.domain.booking.dto.PublicBusinessDetailResponse;
import io.moer.booking.domain.booking.dto.PublicBusinessListResponse;
import io.moer.booking.domain.booking.dto.PublicBusinessSearchCondition;
import io.moer.booking.domain.booking.dto.SlugCheckResponse;
import io.moer.booking.domain.booking.service.PublicBusinessService;
import io.moer.booking.domain.service.dto.ServiceImageResponse;
import io.moer.booking.domain.service.service.ServiceImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 고객용 Public 매장 API
 * 인증 없이 접근 가능
 */
@RestController
@RequestMapping("/api/public/businesses")
@RequiredArgsConstructor
@Tag(name = "Public Business", description = "고객용 매장 검색/조회 API (인증 불필요)")
public class PublicBusinessController {

    private final PublicBusinessService publicBusinessService;
    private final ServiceImageService serviceImageService;

    /**
     * 플랫폼 통계 (랜딩 페이지용)
     */
    @GetMapping("/platform-stats")
    @Operation(
            summary = "플랫폼 통계 조회",
            description = "랜딩 페이지에 표시할 플랫폼 전체 통계를 조회합니다. 인증 불필요."
    )
    public ApiResponse<PlatformStatsResponse> getPlatformStats() {
        return ApiResponse.success(publicBusinessService.getPlatformStats());
    }

    /**
     * 매장 검색/목록
     */
    @GetMapping
    @Operation(
            summary = "매장 검색",
            description = "키워드, 업종, 정렬 기준으로 매장을 검색합니다. 인증 불필요."
    )
    public ApiResponse<PageResponse<PublicBusinessListResponse>> searchBusinesses(
            @Parameter(description = "검색 키워드 (매장명, 주소)") @RequestParam(required = false) String keyword,
            @Parameter(description = "업종 필터 (BEAUTY_SHOP, PILATES 등)") @RequestParam(required = false) String businessType,
            @Parameter(description = "정렬 기준 (rating, name, created_at)") @RequestParam(required = false, defaultValue = "rating") String sortBy,
            @Parameter(description = "페이지 번호 (1부터)") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(required = false, defaultValue = "20") Integer size) {

        PublicBusinessSearchCondition condition = PublicBusinessSearchCondition.builder()
                .keyword(keyword)
                .businessType(businessType)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();

        PageResponse<PublicBusinessListResponse> response = publicBusinessService.searchBusinesses(condition);
        return ApiResponse.success(response);
    }

    /**
     * 위치 기반 매장 검색 (주변 매장)
     */
    @GetMapping("/nearby")
    @Operation(
            summary = "주변 매장 검색",
            description = "위도/경도 기반으로 주변 매장을 검색합니다. 거리 순으로 정렬됩니다. 인증 불필요."
    )
    public ApiResponse<Map<String, Object>> searchNearbyBusinesses(
            @Parameter(description = "위도", required = true) @RequestParam Double lat,
            @Parameter(description = "경도", required = true) @RequestParam Double lng,
            @Parameter(description = "검색 반경 (km, 기본 5)") @RequestParam(required = false) Integer radius,
            @Parameter(description = "업종 필터 (BEAUTY_SHOP, PILATES 등)") @RequestParam(required = false) String type,
            @Parameter(description = "매장명 검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호 (1부터)") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(required = false, defaultValue = "20") Integer size) {

        NearbyBusinessSearchCondition condition = NearbyBusinessSearchCondition.builder()
                .lat(lat)
                .lng(lng)
                .radius(radius)
                .businessType(type)
                .keyword(keyword)
                .page(page)
                .size(size)
                .build();

        Map<String, Object> response = publicBusinessService.searchNearbyBusinesses(condition);
        return ApiResponse.success(response);
    }

    /**
     * 슬러그 사용 가능 확인
     */
    @GetMapping("/check-slug")
    @Operation(
            summary = "슬러그 사용 가능 확인",
            description = "해당 슬러그를 사용할 수 있는지 확인합니다. 인증 불필요."
    )
    public ApiResponse<SlugCheckResponse> checkSlugAvailability(
            @Parameter(description = "확인할 슬러그", required = true) @RequestParam String slug) {

        SlugCheckResponse response = publicBusinessService.checkSlugAvailability(slug);
        return ApiResponse.success(response);
    }

    /**
     * 매장 상세 조회 (슬러그 또는 ID)
     */
    @GetMapping("/{slugOrId}")
    @Operation(
            summary = "매장 상세 조회",
            description = "슬러그 또는 ID로 매장 상세 정보를 조회합니다. 숫자이면 ID로, 문자열이면 slug로 조회합니다. 인증 불필요."
    )
    public ApiResponse<PublicBusinessDetailResponse> getBusinessDetail(
            @Parameter(description = "매장 슬러그 또는 ID", required = true) @PathVariable String slugOrId) {

        PublicBusinessDetailResponse response = publicBusinessService.getBusinessDetail(slugOrId);
        return ApiResponse.success(response);
    }

    /**
     * 서비스 이미지 목록 조회 (Public)
     */
    @GetMapping("/{slugOrId}/services/{serviceId}/images")
    @Operation(
            summary = "서비스 이미지 목록 조회",
            description = "매장 슬러그(또는 ID)와 서비스 ID로 서비스 이미지 목록을 조회합니다. 인증 불필요."
    )
    public ApiResponse<List<ServiceImageResponse>> getServiceImages(
            @Parameter(description = "매장 슬러그 또는 ID", required = true) @PathVariable String slugOrId,
            @Parameter(description = "서비스 ID", required = true) @PathVariable Long serviceId) {

        // slug/ID로 매장 존재 확인 (getBusinessDetail 내부에서 검증)
        publicBusinessService.getBusinessDetail(slugOrId);

        List<ServiceImageResponse> response = serviceImageService.getImages(serviceId);
        return ApiResponse.success(response);
    }
}
