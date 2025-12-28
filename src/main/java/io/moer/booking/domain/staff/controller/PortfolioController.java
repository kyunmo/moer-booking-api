package io.moer.booking.domain.staff.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.staff.dto.PortfolioCreateRequest;
import io.moer.booking.domain.staff.dto.PortfolioResponse;
import io.moer.booking.domain.staff.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    /**
     * Portfolio 생성
     */
    @PostMapping("/staffs/{staffId}/portfolios")
    public ApiResponse<PortfolioResponse> createPortfolio(
            @PathVariable Long staffId,
            @Valid @RequestBody PortfolioCreateRequest request) {
        PortfolioResponse response = portfolioService.createPortfolio(staffId, request);
        return ApiResponse.success(response);
    }

    /**
     * Staff의 Portfolio 목록 조회 (관리자용 - 전체)
     */
    @GetMapping("/staffs/{staffId}/portfolios")
    public ApiResponse<List<PortfolioResponse>> getPortfoliosByStaff(
            @PathVariable Long staffId) {
        List<PortfolioResponse> response = portfolioService.getPortfoliosByStaff(staffId);
        return ApiResponse.success(response);
    }

    /**
     * Staff의 공개 Portfolio 목록 조회 (고객용)
     */
    @GetMapping("/staffs/{staffId}/portfolios/visible")
    public ApiResponse<List<PortfolioResponse>> getVisiblePortfoliosByStaff(
            @PathVariable Long staffId) {
        List<PortfolioResponse> response = portfolioService.getVisiblePortfoliosByStaff(staffId);
        return ApiResponse.success(response);
    }

    /**
     * Business의 전체 Portfolio 목록 조회
     */
    @GetMapping("/businesses/{businessId}/portfolios")
    public ApiResponse<List<PortfolioResponse>> getPortfoliosByBusiness(
            @PathVariable Long businessId) {
        List<PortfolioResponse> response = portfolioService.getPortfoliosByBusiness(businessId);
        return ApiResponse.success(response);
    }

    /**
     * Portfolio 공개/비공개 전환
     */
    @PatchMapping("/portfolios/{portfolioId}/toggle-visibility")
    public ApiResponse<PortfolioResponse> togglePortfolioVisibility(
            @PathVariable Long portfolioId) {
        PortfolioResponse response = portfolioService.togglePortfolioVisibility(portfolioId);
        return ApiResponse.success(response);
    }

    /**
     * Portfolio 삭제
     */
    @DeleteMapping("/portfolios/{portfolioId}")
    public ApiResponse<Void> deletePortfolio(@PathVariable Long portfolioId) {
        portfolioService.deletePortfolio(portfolioId);
        return ApiResponse.success();
    }
}