package io.moer.booking.domain.staff.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.staff.Staff;
import io.moer.booking.domain.staff.dto.PortfolioResponse;
import io.moer.booking.domain.staff.dto.PublicPortfolioListResponse;
import io.moer.booking.domain.staff.repository.StaffRepository;
import io.moer.booking.domain.staff.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 공개 포트폴리오 API (비인증)
 * 고객이 slug 기반으로 특정 매장의 스태프 포트폴리오를 조회
 */
@RestController
@RequestMapping("/api/public/businesses/{slug}/staffs/{staffId}/portfolios")
@RequiredArgsConstructor
@Tag(name = "Public Portfolio", description = "공개 포트폴리오 조회 API")
public class PublicPortfolioController {

    private final PortfolioService portfolioService;
    private final BusinessRepository businessRepository;
    private final StaffRepository staffRepository;

    @GetMapping
    @Operation(summary = "스태프 공개 포트폴리오 조회", description = "slug 기반으로 특정 매장 스태프의 공개 포트폴리오를 조회합니다")
    public ResponseEntity<ApiResponse<PublicPortfolioListResponse>> getPortfolios(
            @PathVariable String slug,
            @PathVariable Long staffId) {

        // slug -> business 조회
        Business business = businessRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.BUSINESS_NOT_FOUND,
                        "매장을 찾을 수 없습니다: " + slug));

        // staffId 검증 (해당 매장 소속인지)
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.STAFF_NOT_FOUND,
                        "직원을 찾을 수 없습니다: " + staffId));

        if (!staff.getBusinessId().equals(business.getId())) {
            throw new EntityNotFoundException(
                    ErrorCode.STAFF_NOT_FOUND,
                    "해당 매장의 직원이 아닙니다: " + staffId);
        }

        // 공개 포트폴리오 조회
        List<PortfolioResponse> items = portfolioService.getVisiblePortfoliosByStaff(staffId);

        PublicPortfolioListResponse response = PublicPortfolioListResponse.builder()
                .staffName(staff.getName())
                .staffPosition(staff.getPosition())
                .items(items)
                .totalCount(items.size())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
