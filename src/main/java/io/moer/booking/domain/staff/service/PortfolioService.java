package io.moer.booking.domain.staff.service;

import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.staff.Portfolio;
import io.moer.booking.domain.staff.dto.PortfolioCreateRequest;
import io.moer.booking.domain.staff.dto.PortfolioResponse;
import io.moer.booking.domain.staff.repository.PortfolioRepository;
import io.moer.booking.domain.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 포트폴리오 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final StaffRepository staffRepository;

    /**
     * 포트폴리오 생성
     */
    @Transactional
    public PortfolioResponse createPortfolio(Long staffId, PortfolioCreateRequest request) {
        // Staff 존재 확인
        if (!staffRepository.existsById(staffId)) {
            throw new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND);
        }

        // tags List<String> → String 변환
        String tagsString = Portfolio.tagsToString(request.getTags());

        Portfolio portfolio = Portfolio.builder()
                .staffId(staffId)
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .tags(tagsString)
                .isVisible("Y")
                .build();

        portfolioRepository.save(portfolio);

        log.info("Portfolio created: id={}, staffId={}", portfolio.getId(), staffId);

        // 생성된 Portfolio 다시 조회 (businessId 포함)
        Portfolio saved = portfolioRepository.findById(portfolio.getId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.PORTFOLIO_NOT_FOUND));

        return PortfolioResponse.from(saved);
    }

    /**
     * Staff의 전체 Portfolio 조회 (관리자용)
     */
    public List<PortfolioResponse> getPortfoliosByStaff(Long staffId) {
        return portfolioRepository.findByStaffId(staffId).stream()
                .map(PortfolioResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Staff의 공개 Portfolio 조회 (고객용)
     */
    public List<PortfolioResponse> getVisiblePortfoliosByStaff(Long staffId) {
        return portfolioRepository.findVisibleByStaffId(staffId).stream()
                .map(PortfolioResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Business의 전체 Portfolio 조회
     */
    public List<PortfolioResponse> getPortfoliosByBusiness(Long businessId) {
        return portfolioRepository.findByBusinessId(businessId).stream()
                .map(PortfolioResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 포트폴리오 공개/비공개 전환
     */
    @Transactional
    public PortfolioResponse togglePortfolioVisibility(Long portfolioId) {
        if (!portfolioRepository.existsById(portfolioId)) {
            throw new EntityNotFoundException(ErrorCode.PORTFOLIO_NOT_FOUND);
        }

        portfolioRepository.toggleVisibility(portfolioId);

        log.info("Portfolio visibility toggled: id={}", portfolioId);

        // 다시 조회
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.PORTFOLIO_NOT_FOUND));

        return PortfolioResponse.from(portfolio);
    }

    /**
     * 포트폴리오 삭제
     */
    @Transactional
    public void deletePortfolio(Long portfolioId) {
        if (!portfolioRepository.existsById(portfolioId)) {
            throw new EntityNotFoundException(ErrorCode.PORTFOLIO_NOT_FOUND);
        }

        portfolioRepository.delete(portfolioId);

        log.info("Portfolio deleted: id={}", portfolioId);
    }
}